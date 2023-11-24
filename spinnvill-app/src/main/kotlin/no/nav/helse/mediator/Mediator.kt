package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.avviksvurdering.Avviksvurdering
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.db.Database
import no.nav.helse.kafka.MessageHandler
import no.nav.helse.kafka.SammenligningsgrunnlagMessage
import no.nav.helse.kafka.UtkastTilVedtakMessage
import no.nav.helse.kafka.UtkastTilVedtakRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class Mediator(private val rapidsConnection: RapidsConnection, private val database: Database) : MessageHandler {

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        UtkastTilVedtakRiver(rapidsConnection, this)
    }

    override fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage) {
        logg.info("Behandler utkast_til_vedtak for {}", kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId))
        sikkerlogg.info(
            "Behandler utkast_til_vedtak for {}, {}",
            kv("fødselsnummer", utkastTilVedtakMessage.fødselsnummer),
            kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId)
        )
        val behovProducer = BehovProducer(
            aktørId = utkastTilVedtakMessage.aktørId,
            fødselsnummer = utkastTilVedtakMessage.fødselsnummer,
            vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId,
            organisasjonsnummer = utkastTilVedtakMessage.organisasjonsnummer,
            rapidsConnection = rapidsConnection
        )
        val beregningsgrunnlag = Beregningsgrunnlag.opprett(utkastTilVedtakMessage.beregningsgrunnlag.entries.associate { Organisasjonsnummer(it.key) to OmregnetÅrsinntekt(it.value) })
        håndter(beregningsgrunnlag, utkastTilVedtakMessage.skjæringstidspunkt, behovProducer)
        behovProducer.finalize()
    }

    override fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage) {
        TODO("Not yet implemented")
    }

    private fun håndter(beregningsgrunnlag: Beregningsgrunnlag, skjæringstidspunkt: LocalDate, behovProducer: BehovProducer) {
        val avviksvurdering = avviksvurdering()
            ?: return beOmSammenligningsgrunnlag(skjæringstidspunkt, behovProducer)
        avviksvurdering.håndter(beregningsgrunnlag)
    }

    private fun avviksvurdering(): Avviksvurdering? {
        // Finn dto fra database ?: return null
        // Map om til domenemodell og returner avviksvurdering
        return null
    }

    private fun beOmSammenligningsgrunnlag(skjæringstidspunkt: LocalDate, behovProducer: BehovProducer) {
        val tom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        val fom = tom.minusMonths(11)
        behovProducer.sammenligningsgrunnlag(BehovForSammenligningsgrunnlag(fom, tom))
    }
}