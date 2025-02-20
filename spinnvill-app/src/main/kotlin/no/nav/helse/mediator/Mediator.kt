package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Fødselsnummer
import no.nav.helse.MeldingPubliserer
import no.nav.helse.VersjonAvKode
import no.nav.helse.avviksvurdering.*
import no.nav.helse.db.Database
import no.nav.helse.kafka.AvviksvurderingbehovRiver
import no.nav.helse.kafka.MessageHandler
import no.nav.helse.kafka.SammenligningsgrunnlagRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

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
        if (avgjørOmAvviksvurderingBehovSkalHoppesOver(fødselsnummer, behov)) return

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

    private fun avgjørOmAvviksvurderingBehovSkalHoppesOver(
        fødselsnummer: Fødselsnummer,
        behov: AvviksvurderingBehov,
    ): Boolean {
        val ubehandletAvviksvurderingBehov =
            database.finnUbehandletAvviksvurderingBehov(fødselsnummer, behov.skjæringstidspunkt)
        return if (ubehandletAvviksvurderingBehov !== null) {
            if (ubehandletAvviksvurderingBehov.opprettet.isBefore(LocalDateTime.now().minusHours(1))) {
                logg.info("Sletter ubehandlet avviksvurdering-behov ${ubehandletAvviksvurderingBehov.behovId} som er eldre enn en time")
                sikkerlogg.info(
                    "Sletter ubehandlet avviksvurdering-behov ${ubehandletAvviksvurderingBehov.behovId} for {} som er eldre enn en time",
                    kv("fødselsnummer", fødselsnummer.value)
                )
                database.slettAvviksvurderingBehov(ubehandletAvviksvurderingBehov)
                false
            } else {
                logg.info("Ignorerer avviksvurdering-behov, det ble funnet igjen som ubesvart i databasen")
                sikkerlogg.info(
                    "Ignorerer avviksvurdering-behov for {}, det ble funnet igjen som ubesvart i databasen",
                    kv("fødselsnummer", fødselsnummer.value)
                )
                true
            }
        } else {
            false
        }
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
        database.lagreGrunnlagshistorikk(this.grunnlagene())
    }

    private fun AvviksvurderingBehov.lagre() {
        database.lagreAvviksvurderingBehov(this)
    }

    private fun hentGrunnlagshistorikkUtenInfotrygd(
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
    ): Grunnlagshistorikk {
        val grunnlag = database
            .finnAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt)
            .filterNot { it.kilde == Kilde.INFOTRYGD }
        return Grunnlagshistorikk(fødselsnummer, skjæringstidspunkt, grunnlag)
    }

    internal companion object {
        private val logg = LoggerFactory.getLogger(Mediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
