package no.nav.helse

import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.mediator.producer.Message
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class MeldingPubliserer(private val rapidsConnection: RapidsConnection, private val avviksvurderingBehov: AvviksvurderingBehov) {

    private val meldinger = mutableListOf<Message>()

    fun sendMeldinger() {
        meldinger.map { message ->
            when (message) {
                is Message.Behov -> message to JsonMessage.newNeed(message.behov, message.innhold)
                is Message.Hendelse -> message to JsonMessage.newMessage(message.navn, message.innhold)
            }
        }.onEach { (_, json) ->
            json["fødselsnummer"] = avviksvurderingBehov.fødselsnummer.value
            json["organisasjonsnummer"] = avviksvurderingBehov.organisasjonsnummer
            json["skjæringstidspunkt"] = avviksvurderingBehov.skjæringstidspunkt
            json["vedtaksperiodeId"] = avviksvurderingBehov.vedtaksperiodeId
        }.forEach {
            rapidsConnection.publish(it.second.toJson())
        }
    }

    fun subsumsjon() {

    }

    fun avviksvurderingBehovLøsning() {

    }

    fun behovForSammenligningsgrunnlag(behov: BehovForSammenligningsgrunnlag) {
        val behovNavn = "InntekterForSammenligningsgrunnlag"
        meldinger.add(Message.Behov(setOf(behovNavn), mapOf(behovNavn to behov.toMap())))
    }

    private fun BehovForSammenligningsgrunnlag.toMap(): Map<String, Any> {
        return mapOf(
            "beregningStart" to beregningsperiodeFom,
            "beregningSlutt" to beregningsperiodeTom,
            "skjæringstidspunkt" to skjæringstidspunkt
        )
    }
}