package no.nav.helse.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class AvviksvurderingbehovRiver(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAll("@behov", listOf("Avviksvurdering"))
                it.rejectKey("@løsning")
                it.requireKey("fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId")
                it.requireKey("vilkårsgrunnlagId", "skjæringstidspunkt")
                it.requireArray("omregnedeÅrsinntekter") {
                    requireKey("organisasjonsnummer", "beløp")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info(
            "Leser avviksvurderingbehov {}",
            kv("Fødselsnummer", packet["fødselsnummer"].asText())
        )
        messageHandler.håndter(AvviksvurderingbehovMessage(packet))
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
