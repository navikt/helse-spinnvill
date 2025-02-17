package no.nav.helse.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.FeatureToggles
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class GodkjenningsbehovRiver(
    rapidsConnection: RapidsConnection,
    private val messageHandler: MessageHandler,
    private val featureToggles: FeatureToggles
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.rejectKey("@løsning")
                it.rejectKey("behandletAvSpinnvill")
                it.requireKey("fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId")
                it.requireKey("Godkjenning.vilkårsgrunnlagId", "Godkjenning.skjæringstidspunkt")
                it.requireArray("Godkjenning.omregnedeÅrsinntekter") {
                    requireKey("organisasjonsnummer", "beløp")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (featureToggles.skalBenytteNyAvviksvurderingløype()) return
        sikkerlogg.info(
            "Leser godkjenningsbehov {}",
            kv("Fødselsnummer", packet["fødselsnummer"].asText())
        )
        messageHandler.håndter(GodkjenningsbehovMessage(packet))
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
