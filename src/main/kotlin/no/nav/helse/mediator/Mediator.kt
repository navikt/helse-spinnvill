package no.nav.helse.mediator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.db.Dao
import no.nav.helse.kafka.MessageHandler
import no.nav.helse.kafka.UtkastTilVedtakMessage
import no.nav.helse.modell.Sykefraværstilfelle
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate

class Mediator(rapidsConnection: RapidsConnection, private val dao: Dao): MessageHandler {

    override fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage) {
        val beregningsgrunnlag = Beregningsgrunnlag(utkastTilVedtakMessage.beregningsgrunnlag)
        val sykefraværstilfelle = sykefraværstilfelle(utkastTilVedtakMessage.fødselsnummer, utkastTilVedtakMessage.skjæringstidspunkt, beregningsgrunnlag)
        sykefraværstilfelle.nyttUtkastTilVedtak(beregningsgrunnlag)
    }

    private fun sykefraværstilfelle(fødselsnummer: String, skjæringstidspunkt: LocalDate, beregningsgrunnlag: Beregningsgrunnlag): Sykefraværstilfelle {
        val avviksvurderingJson = dao.finnAvviksvurdering(fødselsnummer, skjæringstidspunkt)
        val avviksvurdering = avviksvurderingJson?.let {
            jacksonObjectMapper().readValue(it, Avviksvurdering::class.java)
        } ?: Avviksvurdering.nyAvviksvurdering(beregningsgrunnlag)
        return Sykefraværstilfelle(skjæringstidspunkt, avviksvurdering)
    }
}