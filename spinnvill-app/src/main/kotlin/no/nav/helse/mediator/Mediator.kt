package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.db.Database
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.kafka.*
import no.nav.helse.mediator.producer.*
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate

class Mediator(
    private val versjonAvKode: VersjonAvKode,
    private val rapidsConnection: RapidsConnection,
    private val database: Database
) : MessageHandler {

    init {
        UtkastTilVedtakRiver(rapidsConnection, this)
        SammenligningsgrunnlagRiver(rapidsConnection, this)
        AvviksvurderingerFraSpleisRiver(rapidsConnection, this)
        AvviksvurderingFraSpleisRiver(rapidsConnection, this)
    }

    override fun håndter(enAvviksvurderingFraSpleisMessage: EnAvviksvurderingFraSpleisMessage) {
        if (!Toggle.LesemodusOnly.enabled) return
        logg.info("Mottok avviksvurdering fra Spleis")
        sikkerlogg.info("Mottok avviksvurdering fra Spleis")
        val fødselsnummer = enAvviksvurderingFraSpleisMessage.fødselsnummer
        val meldingProducer = MigrerteAvviksvurderingerProducer(fødselsnummer, rapidsConnection)

        lagreOgVideresendAvviksvurdering(fødselsnummer, enAvviksvurderingFraSpleisMessage.avviksvurdering, meldingProducer)
        meldingProducer.publiserMeldinger()
        logg.info("Avviksvurdering fra Spleis ferdigbehandlet")
        sikkerlogg.info("Avviksvurdering fra Spleis ferdigbehandlet")
    }

    override fun håndter(avviksvurderingerFraSpleisMessage: AvviksvurderingerFraSpleisMessage) {
        logg.info("Mottok migrering med ${avviksvurderingerFraSpleisMessage.avviksvurderinger.size} avviksvurderinger fra Spleis")
        sikkerlogg.info("Mottok migrering med ${avviksvurderingerFraSpleisMessage.avviksvurderinger.size} avviksvurderinger fra Spleis")
        val fødselsnummer = avviksvurderingerFraSpleisMessage.fødselsnummer

        val meldingProducer = MigrerteAvviksvurderingerProducer(fødselsnummer, rapidsConnection)

        avviksvurderingerFraSpleisMessage.avviksvurderinger.forEach { avviksvurdering ->
            lagreOgVideresendAvviksvurdering(fødselsnummer, avviksvurdering, meldingProducer)
        }
        meldingProducer.publiserMeldinger()
        logg.info("Migrering med avviksvurderinger fra Spleis ferdigbehandlet")
        sikkerlogg.info("Migrering med avviksvurderinger fra Spleis ferdigbehandlet")
    }

    override fun håndter(message: UtkastTilVedtakMessage) {
        val meldingProducer = nyMeldingProducer(message)

        if (Toggle.LesemodusOnly.enabled) {
            håndterLesemodus(meldingProducer, message)
            return
        }

        logg.info("Behandler utkast_til_vedtak for {}", kv("vedtaksperiodeId", message.vedtaksperiodeId))
        sikkerlogg.info("Behandler utkast_til_vedtak for {}, {}", kv("fødselsnummer", message.fødselsnummer), kv("vedtaksperiodeId", message.vedtaksperiodeId))

        val behovProducer = BehovProducer(utkastTilVedtakJson = message.toJson())
        val varselProducer = VarselProducer(vedtaksperiodeId = message.vedtaksperiodeId)
        val subsumsjonProducer = nySubsumsjonProducer(message)
        val avvikVurdertProducer = AvvikVurdertProducer(vilkårsgrunnlagId = message.vilkårsgrunnlagId)
        val utkastTilVedtakProducer = UtkastTilVedtakProducer(message)

        meldingProducer.nyProducer(behovProducer, varselProducer, subsumsjonProducer, avvikVurdertProducer, utkastTilVedtakProducer)

        val beregningsgrunnlag = nyttBeregningsgrunnlag(message)
        val avviksvurderinger = hentAvviksvurderinger(Fødselsnummer(message.fødselsnummer), message.skjæringstidspunkt)

        avviksvurderinger.registrer(behovProducer)
        avviksvurderinger.registrer(varselProducer, subsumsjonProducer, avvikVurdertProducer)

        val avviksvurdering = avviksvurderinger.håndterNytt(beregningsgrunnlag)
        if (avviksvurdering != null) utkastTilVedtakProducer.registrerUtkastForUtsending(avviksvurdering)
        avviksvurderinger.lagre()

        meldingProducer.publiserMeldinger()
    }

    override fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage) {
        val fødselsnummer = Fødselsnummer(sammenligningsgrunnlagMessage.fødselsnummer)
        val skjæringstidspunkt = sammenligningsgrunnlagMessage.skjæringstidspunkt

        if (finnAvviksvurdering(fødselsnummer, skjæringstidspunkt) != null) {
            logg.warn("Ignorerer duplikat sammenligningsgrunnlag for eksisterende avviksvurdering")
            sikkerlogg.warn("Ignorerer duplikat sammenligningsgrunnlag for {} {}", kv("fødselsnummer", fødselsnummer.value), kv("skjæringstidspunkt", skjæringstidspunkt))
            return
        }

        val avviksvurderinger = hentAvviksvurderinger(fødselsnummer, skjæringstidspunkt)
        val sammenligningsgrunnlag = nyttSammenligningsgrunnlag(sammenligningsgrunnlagMessage)
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag)
        avviksvurderinger.lagre()

        rapidsConnection.queueReplayMessage(fødselsnummer.value, sammenligningsgrunnlagMessage.utkastTilVedtakJson)
    }

    private fun håndterLesemodus(meldingProducer: MeldingProducer, utkastTilVedtakMessage: UtkastTilVedtakMessage) {
        sikkerlogg.info("Spinnvill er i lesemodus")
        val utkastTilVedtakProducer = UtkastTilVedtakProducer(utkastTilVedtakMessage)
        meldingProducer.nyProducer(utkastTilVedtakProducer)
        val avviksvurdering = database.finnSisteAvviksvurdering(utkastTilVedtakMessage.fødselsnummer.somFnr(), utkastTilVedtakMessage.skjæringstidspunkt)
        if (avviksvurdering != null) {
            utkastTilVedtakProducer.registrerUtkastForUtsending(avviksvurdering.tilDomene())
            meldingProducer.publiserMeldinger()
            sikkerlogg.info(
                "Avviksvurdering finnes, vidersender godkjenningsbehov med avviksvurderingId, {}, {}, {}",
                kv("vilkårsgrunnlagId", utkastTilVedtakMessage.vilkårsgrunnlagId),
                kv("skjæringstidspunkt", utkastTilVedtakMessage.skjæringstidspunkt),
                kv("fødselsnummer", utkastTilVedtakMessage.fødselsnummer)
            )
        } else {
            sikkerlogg.info("Avviksvurdering finnes ikke, vidersender ikke godkjenningsbehov med avviksvurderingId, {}, {}, {}",
                kv("vilkårsgrunnlagId", utkastTilVedtakMessage.vilkårsgrunnlagId),
                kv("skjæringstidspunkt", utkastTilVedtakMessage.skjæringstidspunkt),
                kv("fødselsnummer", utkastTilVedtakMessage.fødselsnummer)
            )
        }
    }

    private fun lagreOgVideresendAvviksvurdering(
        fødselsnummer: Fødselsnummer,
        avviksvurdering: AvviksvurderingFraSpleis,
        meldingProducer: MigrerteAvviksvurderingerProducer
    ) {
        if (database.harAvviksvurderingAllerede(fødselsnummer, avviksvurdering.vilkårsgrunnlagId)) return

        database.lagreAvviksvurdering(avviksvurdering.tilDatabaseDto(fødselsnummer))
        database.opprettKoblingTilVilkårsgrunnlag(fødselsnummer, avviksvurdering.vilkårsgrunnlagId, avviksvurdering.id)

        // sender ikke avvik_vurdert dersom avviksvurderingen er gjort i Infotrygd
        // Vi viser hverken avviksprosent eller sammenligningsgrunnlag i Speil når
        // inngangsvilkårene er vurdert i Infotrygd
        if (avviksvurdering.kilde == Avviksvurderingkilde.INFOTRYGD) return

        meldingProducer.nyAvviksvurdering(avviksvurdering.vilkårsgrunnlagId, avviksvurdering.skjæringstidspunkt, avviksvurdering.tilKafkaDto())
    }

    private fun Avviksvurderinger.lagre() {
        val builder = DatabaseDtoBuilder()
        this.accept(builder)
        database.lagreAvviksvurderinger(builder.buildAll())
    }

    private fun finnAvviksvurdering(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurdering? {
        return database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)?.tilDomene()
    }

    private fun hentAvviksvurderinger(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurderinger {
        val avviksvurderinger = database.finnAvviksvurderinger(fødselsnummer, skjæringstidspunkt).map { it.tilDomene() }
        return Avviksvurderinger(fødselsnummer, skjæringstidspunkt, avviksvurderinger)
    }

    private fun nyMeldingProducer(utkastTilVedtakMessage: UtkastTilVedtakMessage) = MeldingProducer(
        aktørId = utkastTilVedtakMessage.aktørId.somAktørId(),
        fødselsnummer = utkastTilVedtakMessage.fødselsnummer.somFnr(),
        vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId,
        organisasjonsnummer = utkastTilVedtakMessage.organisasjonsnummer.somArbeidsgiverref(),
        skjæringstidspunkt = utkastTilVedtakMessage.skjæringstidspunkt,
        rapidsConnection = rapidsConnection
    )

    private fun nySubsumsjonProducer(utkastTilVedtakMessage: UtkastTilVedtakMessage): SubsumsjonProducer {
        return SubsumsjonProducer(
            fødselsnummer = utkastTilVedtakMessage.fødselsnummer.somFnr(),
            vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId,
            organisasjonsnummer = utkastTilVedtakMessage.organisasjonsnummer.somArbeidsgiverref(),
            vilkårsgrunnlagId = utkastTilVedtakMessage.vilkårsgrunnlagId,
            versjonAvKode = versjonAvKode
        )
    }

    private fun nyttBeregningsgrunnlag(utkastTilVedtakMessage: UtkastTilVedtakMessage): Beregningsgrunnlag {
        return Beregningsgrunnlag.opprett(
            utkastTilVedtakMessage.beregningsgrunnlag.entries.associate {
                Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value)
            }
        )
    }

    private fun nyttSammenligningsgrunnlag(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage): Sammenligningsgrunnlag {
        return Sammenligningsgrunnlag(
            sammenligningsgrunnlagMessage.sammenligningsgrunnlag.map {(arbeidsgiver, inntekter) ->
                ArbeidsgiverInntekt(arbeidsgiver.somArbeidsgiverref(), inntekter.map {
                    ArbeidsgiverInntekt.MånedligInntekt(
                        inntekt = InntektPerMåned(it.beløp),
                        måned = it.årMåned,
                        fordel = it.fordel,
                        beskrivelse = it.beskrivelse,
                        inntektstype = it.inntektstype.tilDomene()
                    )
                })
            }
        )
    }

    private fun AvviksvurderingFraSpleis.tilKafkaDto(): AvvikVurdertProducer.AvviksvurderingDto {
        return AvvikVurdertProducer.AvviksvurderingDto(
            this.id,
            requireNotNull(this.avviksprosent),
            requireNotNull(this.beregningsgrunnlagTotalbeløp),
            requireNotNull(this.sammenligningsgrunnlagTotalbeløp),
            this.omregnedeÅrsinntekter,
            this.innrapporterteInntekter.map { innrapportertInntekt ->
                AvvikVurdertProducer.AvviksvurderingDto.InnrapportertInntektDto(
                    innrapportertInntekt.orgnummer.somArbeidsgiverref(),
                    innrapportertInntekt.inntekter.map { månedligInntekt ->
                        AvvikVurdertProducer.AvviksvurderingDto.MånedligInntektDto(
                            månedligInntekt.måned,
                            InntektPerMåned(månedligInntekt.beløp)
                        )
                    }
                )
            }
        )
    }

    private fun AvviksvurderingFraSpleis.tilDatabaseDto(fødselsnummer: Fødselsnummer): AvviksvurderingDto {
        fun Avviksvurderingkilde.tilKildeDto() = when (this) {
            Avviksvurderingkilde.SPLEIS -> AvviksvurderingDto.KildeDto.SPLEIS
            Avviksvurderingkilde.INFOTRYGD -> AvviksvurderingDto.KildeDto.INFOTRYGD
        }

        return AvviksvurderingDto(
            id = this.id,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = this.skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                innrapporterteInntekter = this.innrapporterteInntekter.associate { innrapportertInntekt ->
                    innrapportertInntekt.orgnummer.somArbeidsgiverref() to innrapportertInntekt.inntekter.map { inntekt ->
                        AvviksvurderingDto.MånedligInntektDto(
                            InntektPerMåned(inntekt.beløp),
                            inntekt.måned,
                            Fordel(inntekt.fordel),
                            Beskrivelse(inntekt.beskrivelse),
                            inntektstype = enumValueOf(inntekt.type)
                        )
                    }
                }
            ),
            opprettet = this.vurderingstidspunkt,
            kilde = this.kilde.tilKildeDto(),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(this.omregnedeÅrsinntekter)
        )
    }

    internal companion object {
        private val logg = LoggerFactory.getLogger(Mediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")


        internal fun AvviksvurderingDto.tilDomene(): Avviksvurdering {
            val beregningsgrunnlag = beregningsgrunnlag?.let {
                Beregningsgrunnlag.opprett(it.omregnedeÅrsinntekter)
            } ?: Beregningsgrunnlag.INGEN

            return Avviksvurdering(
                id = id,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlag = beregningsgrunnlag,
                opprettet = opprettet,
                kilde = this.kilde.tilDomene(),
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    sammenligningsgrunnlag.innrapporterteInntekter.map { (organisasjonsnummer, inntekter) ->
                        ArbeidsgiverInntekt(
                            arbeidsgiverreferanse = organisasjonsnummer,
                            inntekter = inntekter.map {
                                ArbeidsgiverInntekt.MånedligInntekt(
                                    inntekt = it.inntekt,
                                    måned = it.måned,
                                    fordel = it.fordel,
                                    beskrivelse = it.beskrivelse,
                                    inntektstype = it.inntektstype.tilDomene()
                                )
                            }
                        )
                    }
                )
            )
        }

        private fun SammenligningsgrunnlagMessage.Inntektstype.tilDomene(): ArbeidsgiverInntekt.Inntektstype {
            return when (this) {
                SammenligningsgrunnlagMessage.Inntektstype.LØNNSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                SammenligningsgrunnlagMessage.Inntektstype.NÆRINGSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT
                SammenligningsgrunnlagMessage.Inntektstype.PENSJON_ELLER_TRYGD -> ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD
                SammenligningsgrunnlagMessage.Inntektstype.YTELSE_FRA_OFFENTLIGE -> ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE
            }
        }

        private fun AvviksvurderingDto.InntektstypeDto.tilDomene(): ArbeidsgiverInntekt.Inntektstype {
            return when (this) {
                AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT
                AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD -> ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD
                AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE -> ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE
            }
        }

        private fun AvviksvurderingDto.KildeDto.tilDomene() = when (this) {
            AvviksvurderingDto.KildeDto.SPINNVILL -> Kilde.SPINNVILL
            AvviksvurderingDto.KildeDto.SPLEIS -> Kilde.SPLEIS
            AvviksvurderingDto.KildeDto.INFOTRYGD -> Kilde.INFOTRYGD
        }
    }
}
