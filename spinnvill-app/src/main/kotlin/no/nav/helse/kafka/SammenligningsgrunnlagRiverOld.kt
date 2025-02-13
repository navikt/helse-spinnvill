package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

internal class SammenligningsgrunnlagRiverOld(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("InntekterForSammenligningsgrunnlag"))
                it.requireKey("@løsning", "fødselsnummer", "InntekterForSammenligningsgrunnlag.skjæringstidspunkt", "utkastTilVedtak")
                it.requireValue("@final", true)
                it.requireArray("@løsning.InntekterForSammenligningsgrunnlag") {
                    require("årMåned", JsonNode::asYearMonth)
                    requireArray("inntektsliste") {
                        requireKey("beløp")
                        requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                        interestedIn("orgnummer", "fødselsnummer", "fordel", "beskrivelse")
                    }
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        messageHandler.håndter(SammenligningsgrunnlagMessageOld(packet))
    }
}
