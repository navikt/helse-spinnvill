package no.nav.helse.mediator

import no.nav.helse.modell.BehovForSammenligningsgrunnlag
import no.nav.helse.modell.BehovObserver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class BehovProducer(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private val rapidsConnection: RapidsConnection
): BehovObserver {

    private val behovskø = mutableMapOf<String, Map<String, Any>>()

    internal fun finalize() {
        val compositeBehov = JsonMessage.newNeed(
            behovskø.keys,
            mutableMapOf<String, Any>(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "organisasjonsnummer" to organisasjonsnummer,
            ).apply {
                putAll(behovskø)
            }
        ).toJson()
        rapidsConnection.publish(fødselsnummer, compositeBehov)
    }
    override fun sammenligningsgrunnlag(behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) {
        behovskø["InntekterForSammenligningsgrunnlag"] = behovForSammenligningsgrunnlag.toMap()
    }

    private fun BehovForSammenligningsgrunnlag.toMap(): Map<String, Any> {
        return mapOf("beregningStart" to beregningsperiodeFom, "beregningSlutt" to beregningsperiodeTom)
    }
}