package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Fødselsnummer
import no.nav.helse.MeldingPubliserer
import no.nav.helse.VersjonAvKode
import no.nav.helse.avviksvurdering.*
import no.nav.helse.avviksvurdering.Avviksvurderingsgrunnlag.Companion.nyttGrunnlag
import no.nav.helse.db.Database
import no.nav.helse.kafka.AvviksvurderingbehovRiver
import no.nav.helse.kafka.MessageHandler
import no.nav.helse.kafka.SammenligningsgrunnlagRiver
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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
        if (skalHoppesOver(fødselsnummer, behov)) {
            logg.info("Ignorerer avviksvurdering-behov, det ble funnet igjen som ubesvart i databasen")
            sikkerlogg.info(
                "Ignorerer avviksvurdering-behov for {}, det ble funnet igjen som ubesvart i databasen",
                kv("fødselsnummer", fødselsnummer.value)
            )
            return
        }

        logg.info("Behandler avviksvurdering-behov")
        sikkerlogg.info("Behandler avviksvurdering-behov for {}", kv("fødselsnummer", fødselsnummer.value))

        behov.lagre()
        avgjørVidereBehandling(behov, meldingPubliserer)
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
        val nyttGrunnlag = nyttAvviksvurderingsgrunnlag(avviksvurderingBehov, løsning.sammenligningsgrunnlag)
        subsummerOgSvarPåBehov(nyttGrunnlag.avviksvurdering(), meldingPubliserer, avviksvurderingBehov)
        nyttGrunnlag.lagre()
        meldingPubliserer.sendMeldinger()
    }

    private fun avgjørVidereBehandling(behov: AvviksvurderingBehov, meldingPubliserer: MeldingPubliserer) {
        val fødselsnummer = behov.fødselsnummer
        val gjeldendeGrunnlag = gjeldendeAvviksvurderingsgrunnlagOrNull(fødselsnummer, behov.skjæringstidspunkt)
        when {
            gjeldendeGrunnlag == null -> {
                logg.info("Ber om sammenligningsgrunnlag")
                meldingPubliserer.behovForSammenligningsgrunnlag(behovForSammenligningsgrunnlag(behov))
            }
            gjeldendeGrunnlag.beregningsgrunnlagLiktSom(behov.beregningsgrunnlag) -> {
                logg.info("Trenger ikke foreta ny vurdering, beregningsgrunnlaget er likt")
                meldingPubliserer.behovløsningUtenVurdering(gjeldendeGrunnlag.avviksvurdering())
                markerBehovSomLøst(behov)
            }
            else -> {
                val nyttGrunnlag = gjeldendeGrunnlag.kopierMedNyttBeregningsgrunnlag(behov.beregningsgrunnlag)
                subsummerOgSvarPåBehov(nyttGrunnlag.avviksvurdering(), meldingPubliserer, behov)
                nyttGrunnlag.lagre()
            }
        }
    }

    private fun behovForSammenligningsgrunnlag(behov: AvviksvurderingBehov): BehovForSammenligningsgrunnlag {
        val tom = YearMonth.from(behov.skjæringstidspunkt).minusMonths(1)
        val fom = tom.minusMonths(11)
        return BehovForSammenligningsgrunnlag(
            skjæringstidspunkt = behov.skjæringstidspunkt,
            beregningsperiodeFom = fom,
            beregningsperiodeTom = tom
        )
    }

    private fun skalHoppesOver(fødselsnummer: Fødselsnummer, behov: AvviksvurderingBehov): Boolean {
        val ubehandletBehov =
            database.finnUbehandletAvviksvurderingBehov(fødselsnummer, behov.skjæringstidspunkt) ?: return false
        if (!ubehandletBehov.opprettet.isBefore(LocalDateTime.now().minusHours(1))) return true
        logg.info("Sletter ubehandlet avviksvurdering-behov ${ubehandletBehov.behovId} som er eldre enn en time")
        sikkerlogg.info(
            "Sletter ubehandlet avviksvurdering-behov ${ubehandletBehov.behovId} for {} som er eldre enn en time",
            kv("fødselsnummer", fødselsnummer.value)
        )
        database.slettAvviksvurderingBehov(ubehandletBehov)
        return false
    }

    private fun subsummerOgSvarPåBehov(
        resultat: Avviksvurdering,
        meldingPubliserer: MeldingPubliserer,
        avviksvurderingBehov: AvviksvurderingBehov,
    ) {
        meldingPubliserer.`8-30 ledd 2 punktum 1`(resultat)
        if (resultat.harAkseptabeltAvvik) meldingPubliserer.`8-30 ledd 1`(resultat.beregningsgrunnlag)
        meldingPubliserer.behovløsningMedVurdering(resultat)
        logg.info("Ny vurdering foretatt")
        markerBehovSomLøst(avviksvurderingBehov)
    }

    private fun markerBehovSomLøst(behov: AvviksvurderingBehov) {
        behov.løs()
        behov.lagre()
    }

    private fun nyttAvviksvurderingsgrunnlag(
        behov: AvviksvurderingBehov,
        sammenligningsgrunnlag: Sammenligningsgrunnlag
    ): Avviksvurderingsgrunnlag {
        return nyttGrunnlag(
            fødselsnummer = behov.fødselsnummer,
            skjæringstidspunkt = behov.skjæringstidspunkt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = behov.beregningsgrunnlag
        )
    }

    private fun Avviksvurderingsgrunnlag.lagre() {
        database.lagreGrunnlagshistorikk(listOf(this))
    }

    private fun AvviksvurderingBehov.lagre() {
        database.lagreAvviksvurderingBehov(this)
    }

    private fun gjeldendeAvviksvurderingsgrunnlagOrNull(
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate
    ): Avviksvurderingsgrunnlag? {
        return database
            .finnAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt)
            .lastOrNull()
    }

    internal companion object {
        private val logg = LoggerFactory.getLogger(Mediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
