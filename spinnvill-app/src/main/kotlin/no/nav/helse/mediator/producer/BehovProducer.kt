package no.nav.helse.mediator.producer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag

internal class BehovProducer(
    private val utkastTilVedtakJson: JsonNode,
): Producer {
    private val behovskø = mutableMapOf<String, Map<String, Any>>()

    override fun ferdigstill(): List<Message> {
        if (behovskø.isEmpty()) return emptyList()
        val compositeBehov = Message.Behov(
            behov = behovskø.keys.toSet(),
            innhold = mutableMapOf<String, Any>(
                "utkastTilVedtak" to utkastTilVedtakJson
            ).apply {
                putAll(behovskø)
            }
        )
        behovskø.clear()
        return listOf(compositeBehov)
    }

    fun sammenligningsgrunnlag(behov: BehovForSammenligningsgrunnlag) {
        behovskø["InntekterForSammenligningsgrunnlag"] = behov.toMap()
    }

    private fun BehovForSammenligningsgrunnlag.toMap(): Map<String, Any> {
        return mapOf(
            "beregningStart" to beregningsperiodeFom,
            "beregningSlutt" to beregningsperiodeTom,
            "skjæringstidspunkt" to skjæringstidspunkt
        )
    }
}
