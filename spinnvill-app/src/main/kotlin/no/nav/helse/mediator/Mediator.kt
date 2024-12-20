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
    databaseProvider: () -> Database,
) : MessageHandler {

    private val database by lazy(databaseProvider)

    init {
        GodkjenningsbehovRiver(rapidsConnection, this)
        FastsattIInfotrygdRiver(rapidsConnection)
        SammenligningsgrunnlagRiver(rapidsConnection, this)
    }

    override fun håndter(message: FastsattISpleis) {
        val meldingProducer = nyMeldingProducer(message)

        logg.info("Behandler utkast_til_vedtak for {}", kv("vedtaksperiodeId", message.vedtaksperiodeId))
        sikkerlogg.info("Behandler utkast_til_vedtak for {}, {}", kv("fødselsnummer", message.fødselsnummer), kv("vedtaksperiodeId", message.vedtaksperiodeId))

        val behovProducer = BehovProducer(utkastTilVedtakJson = message.toJson())
        val varselProducer = VarselProducer(vedtaksperiodeId = message.vedtaksperiodeId)
        val subsumsjonProducer = nySubsumsjonProducer(message)
        val avvikVurdertProducer = AvvikVurdertProducer(vilkårsgrunnlagId = message.vilkårsgrunnlagId)
        val godkjenningsbehovProducer = GodkjenningsbehovProducer(message)

        meldingProducer.nyProducer(behovProducer, varselProducer, subsumsjonProducer, avvikVurdertProducer, godkjenningsbehovProducer)

        val beregningsgrunnlag = nyttBeregningsgrunnlag(message)
        val avviksvurderinger = hentAvviksvurderinger(Fødselsnummer(message.fødselsnummer), message.skjæringstidspunkt)

        avviksvurderinger.registrer(behovProducer)
        avviksvurderinger.registrer(varselProducer, subsumsjonProducer, avvikVurdertProducer)

        val avviksvurdering = avviksvurderinger.håndterNytt(beregningsgrunnlag)
        if (avviksvurdering != null) godkjenningsbehovProducer.registrerGodkjenningsbehovForUtsending(avviksvurdering)
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

    private fun nyMeldingProducer(godkjenningsbehovMessage: FastsattISpleis) = MeldingProducer(
        fødselsnummer = godkjenningsbehovMessage.fødselsnummer.somFnr(),
        organisasjonsnummer = godkjenningsbehovMessage.organisasjonsnummer.somArbeidsgiverref(),
        skjæringstidspunkt = godkjenningsbehovMessage.skjæringstidspunkt,
        vedtaksperiodeId = godkjenningsbehovMessage.vedtaksperiodeId,
        rapidsConnection = rapidsConnection
    )

    private fun nySubsumsjonProducer(godkjenningsbehovMessage: FastsattISpleis): SubsumsjonProducer {
        return SubsumsjonProducer(
            fødselsnummer = godkjenningsbehovMessage.fødselsnummer.somFnr(),
            vedtaksperiodeId = godkjenningsbehovMessage.vedtaksperiodeId,
            organisasjonsnummer = godkjenningsbehovMessage.organisasjonsnummer.somArbeidsgiverref(),
            vilkårsgrunnlagId = godkjenningsbehovMessage.vilkårsgrunnlagId,
            versjonAvKode = versjonAvKode
        )
    }

    private fun nyttBeregningsgrunnlag(godkjenningsbehovMessage: FastsattISpleis): Beregningsgrunnlag {
        return Beregningsgrunnlag.opprett(
            godkjenningsbehovMessage.beregningsgrunnlag.entries.associate {
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

    internal companion object {
        private val logg = LoggerFactory.getLogger(Mediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")


        internal fun AvviksvurderingDto.tilDomene(): Avviksvurdering {
            val beregningsgrunnlag = beregningsgrunnlag?.let {
                Beregningsgrunnlag.opprett(it.omregnedeÅrsinntekter)
            } ?: Ingen

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
