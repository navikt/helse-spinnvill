package no.nav.helse.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class UtkastTilVedtakRiver(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.rejectKey("@løsning")

                it.requireKey("fødselsnummer", "organisasjonsnummer", "aktørId", "vedtaksperiodeId", "Godkjenning.skjæringstidspunkt")
                it.requireArray("Godkjenning.omregnedeÅrsinntekter") {
                    requireKey("organisasjonsnummer", "beløp")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info(
            "Leser godkjenningsbehov {}",
            kv("Fødselsnummer", packet["fødselsnummer"].asText())
        )
        messageHandler.håndter(UtkastTilVedtakMessage(packet))
        //hentDataFraDb
        //lag sykefraværstilfelle
        //fisk ut beregningsgrunnlaget fra utkast til vedtak (omregnede årsinntekter)
        //spør sykefraværstilfelle om det trenger data
        //hvis ja lag og send ut behov
        //hvis nei få avviksprosenten fra sykefraværstilfelle
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}