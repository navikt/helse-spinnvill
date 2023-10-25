package no.nav.helse

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class UtkastTilVedtakRiver(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.rejectKey("@løsning")

                it.requireKey("fødselsnummer", "Godkjenning.skjæringstidspunkt")
                it.requireArray("Godkjenning.omregnedeÅrsinntekter") {
                    requireKey("organisasjonsnummer", "beløp")
                }
                it.requireArray("sammenligningsgrunnlag")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info(
            "Leser godkjenningsbehov {}",
            kv("Fødselsnummer", packet["fødselsnummer"].asText())
        )
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
