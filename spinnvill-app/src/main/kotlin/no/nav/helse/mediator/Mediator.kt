package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.db.Database
import no.nav.helse.db.DatabaseDtoBuilder
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.kafka.*
import no.nav.helse.mediator.producer.*
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate

class Mediator(
    private val versjonAvKode: VersjonAvKode,
    private val rapidsConnection: RapidsConnection,
    featureToggles: FeatureToggles,
    databaseProvider: () -> Database,
) : MessageHandler {

    private val database by lazy(databaseProvider)

    init {
        GodkjenningsbehovRiver(rapidsConnection, this, featureToggles)
        SammenligningsgrunnlagRiverOld(rapidsConnection, this)
        SammenligningsgrunnlagRiver(rapidsConnection, this)
        AvviksvurderingbehovRiver(rapidsConnection, this)
    }

    @Deprecated("Tilhører gammel løype")
    override fun håndter(message: GodkjenningsbehovMessage) {
        val meldingProducer = nyMeldingProducer(message)

        logg.info("Behandler utkast_til_vedtak for {}", kv("vedtaksperiodeId", message.vedtaksperiodeId))
        sikkerlogg.info(
            "Behandler utkast_til_vedtak for {}, {}",
            kv("fødselsnummer", message.fødselsnummer),
            kv("vedtaksperiodeId", message.vedtaksperiodeId)
        )

        val behovProducer = BehovProducer(utkastTilVedtakJson = message.toJson())
        val varselProducer = VarselProducer(vedtaksperiodeId = message.vedtaksperiodeId)
        val subsumsjonProducer = nySubsumsjonProducer(message)
        val avvikVurdertProducer = AvvikVurdertProducer(vilkårsgrunnlagId = message.vilkårsgrunnlagId)
        val godkjenningsbehovProducer = GodkjenningsbehovProducer(message)

        meldingProducer.nyProducer(
            behovProducer,
            varselProducer,
            subsumsjonProducer,
            avvikVurdertProducer,
            godkjenningsbehovProducer
        )

        val beregningsgrunnlag = nyttBeregningsgrunnlag(message)
        val avviksvurderinger = hentGrunnlagshistorikk(Fødselsnummer(message.fødselsnummer), message.skjæringstidspunkt)

        when (val resultat = avviksvurderinger.håndterNytt(beregningsgrunnlag)) {
            is Avviksvurderingsresultat.TrengerSammenligningsgrunnlag -> behovProducer.sammenligningsgrunnlag(resultat.behov)
            is Avviksvurderingsresultat.AvvikVurdert -> {
                val vurdertAvvik = resultat.vurdering
                godkjenningsbehovProducer.registrerGodkjenningsbehovForUtsending(resultat.grunnlag)
                subsumsjonProducer.avvikVurdert(vurdertAvvik)
                avvikVurdertProducer.avvikVurdert(vurdertAvvik)
                varselProducer.avvikVurdert(vurdertAvvik.harAkseptabeltAvvik, vurdertAvvik.avviksprosent)
            }

            is Avviksvurderingsresultat.TrengerIkkeNyVurdering -> {
                godkjenningsbehovProducer.registrerGodkjenningsbehovForUtsending(resultat.gjeldendeGrunnlag)
            }
        }
        avviksvurderinger.lagre()
        meldingProducer.publiserMeldinger()
    }

    @Deprecated("Tilhører gammel løype")
    override fun håndter(sammenligningsgrunnlagMessageOld: SammenligningsgrunnlagMessageOld) {
        val fødselsnummer = Fødselsnummer(sammenligningsgrunnlagMessageOld.fødselsnummer)
        val skjæringstidspunkt = sammenligningsgrunnlagMessageOld.skjæringstidspunkt

        if (finnAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt) != null) {
            logg.warn("Ignorerer duplikat sammenligningsgrunnlag for eksisterende avviksvurdering")
            sikkerlogg.warn(
                "Ignorerer duplikat sammenligningsgrunnlag for {} {}",
                kv("fødselsnummer", fødselsnummer.value),
                kv("skjæringstidspunkt", skjæringstidspunkt)
            )
            return
        }

        val avviksvurderinger = hentGrunnlagshistorikk(fødselsnummer, skjæringstidspunkt)
        val sammenligningsgrunnlag = nyttSammenligningsgrunnlag(sammenligningsgrunnlagMessageOld)
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag)
        avviksvurderinger.lagre()

        rapidsConnection.queueReplayMessage(fødselsnummer.value, sammenligningsgrunnlagMessageOld.utkastTilVedtakJson)
    }

    override fun håndter(behov: AvviksvurderingBehov) {
        val meldingPubliserer = MeldingPubliserer(rapidsConnection, behov, versjonAvKode)
        if (harUbesvartBehov(behov.fødselsnummer, behov.skjæringstidspunkt)) return

        behov.lagre()
        val avviksvurderinger = hentGrunnlagshistorikkUtenInfotrygd(behov.fødselsnummer, behov.skjæringstidspunkt)

        when (val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag = behov.beregningsgrunnlag)) {
            is Avviksvurderingsresultat.TrengerSammenligningsgrunnlag -> meldingPubliserer.behovForSammenligningsgrunnlag(
                resultat.behov
            )

            is Avviksvurderingsresultat.TrengerIkkeNyVurdering -> {
                meldingPubliserer.behovløsningUtenVurdering(resultat.gjeldendeGrunnlag.id)
                markerBehovSomLøst(behov)
            }

            is Avviksvurderingsresultat.AvvikVurdert -> {
                subsummerOgSvarPåBehov(resultat, meldingPubliserer, behov)
            }
        }
        avviksvurderinger.lagre()
        meldingPubliserer.sendMeldinger()
    }

    override fun håndter(sammenligningsgrunnlagLøsning: SammenligningsgrunnlagLøsning) {
        val fødselsnummer = sammenligningsgrunnlagLøsning.fødselsnummer
        val skjæringstidspunkt = sammenligningsgrunnlagLøsning.skjæringstidspunkt

        val avviksvurderingBehov =
            database.finnUbehandletAvviksvurderingBehov(fødselsnummer, skjæringstidspunkt) ?: return
        if (avviksvurderingBehov.behovId != sammenligningsgrunnlagLøsning.avviksvurderingBehovId) return

        val meldingPubliserer = MeldingPubliserer(rapidsConnection, avviksvurderingBehov, versjonAvKode)
        val avviksvurderinger = hentGrunnlagshistorikk(fødselsnummer, skjæringstidspunkt)
        val resultat = avviksvurderinger.nyttSammenligningsgrunnlag(
            sammenligningsgrunnlag = sammenligningsgrunnlagLøsning.sammenligningsgrunnlag,
            beregningsgrunnlag = avviksvurderingBehov.beregningsgrunnlag
        )
        subsummerOgSvarPåBehov(resultat, meldingPubliserer, avviksvurderingBehov)
        avviksvurderinger.lagre()
        meldingPubliserer.sendMeldinger()
    }

    private fun subsummerOgSvarPåBehov(
        resultat: Avviksvurderingsresultat.AvvikVurdert,
        meldingPubliserer: MeldingPubliserer,
        avviksvurderingBehov: AvviksvurderingBehov,
    ) {
        val avviksvurdering = resultat.vurdering
        meldingPubliserer.`8-30 ledd 2 punktum 1`(avviksvurdering)
        if (avviksvurdering.harAkseptabeltAvvik) meldingPubliserer.`8-30 ledd 1`(avviksvurdering.beregningsgrunnlag)
        meldingPubliserer.behovløsningMedVurdering(avviksvurdering)
        markerBehovSomLøst(avviksvurderingBehov)
    }

    private fun markerBehovSomLøst(behov: AvviksvurderingBehov) {
        behov.løs()
        behov.lagre()
    }

    private fun Grunnlagshistorikk.lagre() {
        val builder = DatabaseDtoBuilder()
        database.lagreGrunnlagshistorikk(builder.buildAll(grunnlagene()))
    }

    private fun AvviksvurderingBehov.lagre() {
        database.lagreAvviksvurderingBehov(this)
    }

    private fun finnAvviksvurderingsgrunnlag(
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
    ): Avviksvurderingsgrunnlag? {
        return database.finnSisteAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt)?.tilDomene()
    }

    private fun harUbesvartBehov(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Boolean {
        return database.finnUbehandletAvviksvurderingBehov(fødselsnummer, skjæringstidspunkt) != null
    }

    private fun hentGrunnlagshistorikk(
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
    ): Grunnlagshistorikk {
        val grunnlag = database.finnAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt).map { it.tilDomene() }
        return Grunnlagshistorikk(fødselsnummer, skjæringstidspunkt, grunnlag)
    }

    private fun hentGrunnlagshistorikkUtenInfotrygd(
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
    ): Grunnlagshistorikk {
        val grunnlag = database.finnAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt)
            .filterNot { it.kilde == AvviksvurderingDto.KildeDto.INFOTRYGD }
            .map { it.tilDomene() }
        return Grunnlagshistorikk(fødselsnummer, skjæringstidspunkt, grunnlag)
    }

    private fun nyMeldingProducer(godkjenningsbehovMessage: GodkjenningsbehovMessage) = MeldingProducer(
        fødselsnummer = godkjenningsbehovMessage.fødselsnummer.somFnr(),
        organisasjonsnummer = godkjenningsbehovMessage.organisasjonsnummer.somArbeidsgiverref(),
        skjæringstidspunkt = godkjenningsbehovMessage.skjæringstidspunkt,
        vedtaksperiodeId = godkjenningsbehovMessage.vedtaksperiodeId,
        rapidsConnection = rapidsConnection
    )

    private fun nySubsumsjonProducer(godkjenningsbehovMessage: GodkjenningsbehovMessage): SubsumsjonProducer {
        return SubsumsjonProducer(
            fødselsnummer = godkjenningsbehovMessage.fødselsnummer.somFnr(),
            vedtaksperiodeId = godkjenningsbehovMessage.vedtaksperiodeId,
            organisasjonsnummer = godkjenningsbehovMessage.organisasjonsnummer.somArbeidsgiverref(),
            vilkårsgrunnlagId = godkjenningsbehovMessage.vilkårsgrunnlagId,
            versjonAvKode = versjonAvKode
        )
    }

    private fun nyttBeregningsgrunnlag(godkjenningsbehovMessage: GodkjenningsbehovMessage): Beregningsgrunnlag {
        return Beregningsgrunnlag.opprett(
            godkjenningsbehovMessage.beregningsgrunnlag.entries.associate {
                Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value)
            }
        )
    }

    private fun nyttSammenligningsgrunnlag(sammenligningsgrunnlagMessageOld: SammenligningsgrunnlagMessageOld): Sammenligningsgrunnlag {
        return Sammenligningsgrunnlag(
            sammenligningsgrunnlagMessageOld.sammenligningsgrunnlag.map { (arbeidsgiver, inntekter) ->
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


        internal fun AvviksvurderingDto.tilDomene(): Avviksvurderingsgrunnlag {
            val beregningsgrunnlag = beregningsgrunnlag?.let {
                Beregningsgrunnlag.opprett(it.omregnedeÅrsinntekter)
            } ?: Ingen

            return Avviksvurderingsgrunnlag(
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

        private fun SammenligningsgrunnlagMessageOld.Inntektstype.tilDomene(): ArbeidsgiverInntekt.Inntektstype {
            return when (this) {
                SammenligningsgrunnlagMessageOld.Inntektstype.LØNNSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                SammenligningsgrunnlagMessageOld.Inntektstype.NÆRINGSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT
                SammenligningsgrunnlagMessageOld.Inntektstype.PENSJON_ELLER_TRYGD -> ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD
                SammenligningsgrunnlagMessageOld.Inntektstype.YTELSE_FRA_OFFENTLIGE -> ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE
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
