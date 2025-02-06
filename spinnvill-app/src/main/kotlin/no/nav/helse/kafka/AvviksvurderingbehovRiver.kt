package no.nav.helse.kafka

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

internal class AvviksvurderingbehovRiver(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAll("@behov", listOf("Avviksvurdering"))
                it.rejectKey("@løsning")
                it.requireKey("fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "@behovId")
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
        messageHandler.håndter(
            AvviksvurderingBehov(
                packet["vilkårsgrunnlagId"].asUUID(),
                packet["@behovId"].asUUID(),
                packet["skjæringstidspunkt"].asLocalDate(),
                Fødselsnummer(packet["fødselsnummer"].asText()),
                packet["vedtaksperiodeId"].asUUID(),
                packet["organisasjonsnummer"].asText(),
                Beregningsgrunnlag.opprett(packet["omregnedeÅrsinntekter"].associate {
                    Arbeidsgiverreferanse(it["organisasjonsnummer"].asText()) to OmregnetÅrsinntekt(it["beløp"].asDouble())
                })
            )
        )
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
