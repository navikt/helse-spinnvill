package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SammenligningsgrunnlagRiver(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("InntekterForSammenligningsgrunnlag"))
                it.requireKey("@løsning", "fødselsnummer", "skjæringstidspunkt")
                it.requireArray("@løsning.InntekterForSammenligningsgrunnlag") {
                    requireKey("årMåned")
                    requireArray("inntektsliste") {
                        requireKey("beløp", "orgnummer")
                    }
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        messageHandler.håndter(SammenligningsgrunnlagMessage(packet))
    }
}
