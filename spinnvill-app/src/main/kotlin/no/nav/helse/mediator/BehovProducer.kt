package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

class BehovProducer(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private val utkastTilVedtakJson: JsonNode,
    private val rapidsConnection: RapidsConnection
) {

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    private val behovskø = mutableMapOf<String, Map<String, Any>>()

    internal fun finalize() {
        if (behovskø.isEmpty()) return
        val compositeBehov = JsonMessage.newNeed(
            behovskø.keys,
            mutableMapOf<String, Any>(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "organisasjonsnummer" to organisasjonsnummer,
                "utkastTilVedtak" to utkastTilVedtakJson
            ).apply {
                putAll(behovskø)
            }
        ).toJson()
        logg.info("Etterspør {} for {}",
            kv("behov", behovskø.keys),
            kv("vedtaksperiodeId", vedtaksperiodeId),
        )
        sikkerlogg.info("Etterspør {} for {}, {}. Behov: {}",
            kv("behov", behovskø.keys),
            kv("fødselsnummer", fødselsnummer),
            kv("vedtaksperiodeId", vedtaksperiodeId),
            compositeBehov
        )
        rapidsConnection.publish(fødselsnummer, compositeBehov)
    }
    internal fun sammenligningsgrunnlag(behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) {
        behovskø["InntekterForSammenligningsgrunnlag"] = behovForSammenligningsgrunnlag.toMap()
    }

    private fun BehovForSammenligningsgrunnlag.toMap(): Map<String, Any> {
        return mapOf(
            "beregningStart" to beregningsperiodeFom,
            "beregningSlutt" to beregningsperiodeTom,
            "skjæringstidspunkt" to skjæringstidspunkt
        )
    }
}