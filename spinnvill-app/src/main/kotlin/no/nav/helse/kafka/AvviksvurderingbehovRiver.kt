package no.nav.helse.kafka

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.somArbeidsgiverref
import no.nav.helse.somFnr
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
                it.requireKey("fødselsnummer", "@behovId")
                it.requireKey("Avviksvurdering.vilkårsgrunnlagId", "Avviksvurdering.skjæringstidspunkt", "Avviksvurdering.organisasjonsnummer", "Avviksvurdering.vedtaksperiodeId")
                it.requireArray("Avviksvurdering.omregnedeÅrsinntekter") {
                    requireKey("organisasjonsnummer", "beløp")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        logg.info("Leser avviksvurdering-behov")
        sikkerlogg.info(
            "Leser avviksvurdering-behov for {}",
            kv("fødselsnummer", packet["fødselsnummer"].asText())
        )
        messageHandler.håndter(
            AvviksvurderingBehov.nyttBehov(
                vilkårsgrunnlagId = packet["Avviksvurdering.vilkårsgrunnlagId"].asUUID(),
                behovId = packet["@behovId"].asUUID(),
                skjæringstidspunkt = packet["Avviksvurdering.skjæringstidspunkt"].asLocalDate(),
                fødselsnummer = packet["fødselsnummer"].asText().somFnr(),
                vedtaksperiodeId = packet["Avviksvurdering.vedtaksperiodeId"].asUUID(),
                organisasjonsnummer = packet["Avviksvurdering.organisasjonsnummer"].asText().somArbeidsgiverref(),
                beregningsgrunnlag = Beregningsgrunnlag(packet["Avviksvurdering.omregnedeÅrsinntekter"].associate {
                    Arbeidsgiverreferanse(it["organisasjonsnummer"].asText()) to OmregnetÅrsinntekt(it["beløp"].asDouble())
                }),
                json = mapper.readValue(packet.toJson())
            )
        )
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }
}
