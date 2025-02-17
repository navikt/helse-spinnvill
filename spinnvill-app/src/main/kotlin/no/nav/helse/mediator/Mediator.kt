package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Fødselsnummer
import no.nav.helse.MeldingPubliserer
import no.nav.helse.VersjonAvKode
import no.nav.helse.avviksvurdering.*
import no.nav.helse.db.Database
import no.nav.helse.db.DatabaseDtoBuilder
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.kafka.AvviksvurderingbehovRiver
import no.nav.helse.kafka.MessageHandler
import no.nav.helse.kafka.SammenligningsgrunnlagRiver
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
        SammenligningsgrunnlagRiver(rapidsConnection, this)
        AvviksvurderingbehovRiver(rapidsConnection, this)
    }

    override fun håndter(behov: AvviksvurderingBehov) {
        val fødselsnummer = behov.fødselsnummer
        val meldingPubliserer = MeldingPubliserer(rapidsConnection, behov, versjonAvKode)
        if (harUbesvartBehov(fødselsnummer, behov.skjæringstidspunkt)) return

        logg.info("Behandler avviksvurdering-behov")
        sikkerlogg.info("Behandler avviksvurdering-behov for {}", kv("fødselsnummer", fødselsnummer.value))

        behov.lagre()
        val avviksvurderinger = hentGrunnlagshistorikkUtenInfotrygd(fødselsnummer, behov.skjæringstidspunkt)

        when (val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag = behov.beregningsgrunnlag)) {
            is Avviksvurderingsresultat.TrengerSammenligningsgrunnlag -> {
                logg.info("Ber om sammenligningsgrunnlag")
                meldingPubliserer.behovForSammenligningsgrunnlag(resultat.behov)
            }

            is Avviksvurderingsresultat.TrengerIkkeNyVurdering -> {
                logg.info("Trenger ikke foreta ny vurdering")
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

    override fun håndter(løsning: SammenligningsgrunnlagLøsning) {
        val fødselsnummer = løsning.fødselsnummer
        val skjæringstidspunkt = løsning.skjæringstidspunkt

        val avviksvurderingBehov =
            database.finnUbehandletAvviksvurderingBehov(fødselsnummer, skjæringstidspunkt) ?: return
        if (avviksvurderingBehov.behovId != løsning.avviksvurderingBehovId) return

        logg.info("Behandler sammenligningsgrunnlag-løsning")
        sikkerlogg.info("Behandler sammenligningsgrunnlag-løsning for {}", kv("fødselsnummer", fødselsnummer.value))

        val meldingPubliserer = MeldingPubliserer(rapidsConnection, avviksvurderingBehov, versjonAvKode)
        val avviksvurderinger = hentGrunnlagshistorikkUtenInfotrygd(fødselsnummer, skjæringstidspunkt)
        val resultat = avviksvurderinger.nyttSammenligningsgrunnlag(
            sammenligningsgrunnlag = løsning.sammenligningsgrunnlag,
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
        logg.info("Ny vurdering foretatt")
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

    private fun harUbesvartBehov(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Boolean {
        return database.finnUbehandletAvviksvurderingBehov(fødselsnummer, skjæringstidspunkt) != null
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
