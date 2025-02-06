package no.nav.helse.kafka

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

internal class AvviksvurderingbehovRiver(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    private val mapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())
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
            AvviksvurderingBehov.nyttBehov(
                vilkårsgrunnlagId = packet["vilkårsgrunnlagId"].asUUID(),
                behovId = packet["@behovId"].asUUID(),
                skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
                fødselsnummer = Fødselsnummer(packet["fødselsnummer"].asText()),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                beregningsgrunnlag = Beregningsgrunnlag.opprett(packet["omregnedeÅrsinntekter"].associate {
                    Arbeidsgiverreferanse(it["organisasjonsnummer"].asText()) to OmregnetÅrsinntekt(it["beløp"].asDouble())
                }),
                json = mapper.readValue(packet.toJson())
            )
        )
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
