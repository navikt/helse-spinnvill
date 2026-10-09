package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
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
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

internal class AvviksvurderingbehovRiver(
    rapidsConnection: RapidsConnection,
    private val messageHandler: MessageHandler,
) : River.PacketListener {
    private val mapper =
        jacksonMapperBuilder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
            .build()

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAll("@behov", listOf("Avviksvurdering"))
                    it.forbid("@løsning")
                }
                validate {
                    it.requireKey("fødselsnummer", "@behovId")
                    it.requireKey("Avviksvurdering.vilkårsgrunnlagId", "Avviksvurdering.skjæringstidspunkt", "Avviksvurdering.organisasjonsnummer", "Avviksvurdering.vedtaksperiodeId")
                    it.requireArray("Avviksvurdering.omregnedeÅrsinntekter") {
                        requireKey("organisasjonsnummer", "beløp")
                    }
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logg.info("Leser avviksvurdering-behov")
        sikkerlogg.info(
            "Leser avviksvurdering-behov for {}",
            kv("fødselsnummer", packet["fødselsnummer"].asString()),
        )
        messageHandler.håndter(
            AvviksvurderingBehov.nyttBehov(
                vilkårsgrunnlagId = packet["Avviksvurdering.vilkårsgrunnlagId"].asUUID(),
                behovId = packet["@behovId"].asUUID(),
                skjæringstidspunkt = packet["Avviksvurdering.skjæringstidspunkt"].asLocalDate(),
                fødselsnummer = packet["fødselsnummer"].asString().somFnr(),
                vedtaksperiodeId = packet["Avviksvurdering.vedtaksperiodeId"].asUUID(),
                organisasjonsnummer = packet["Avviksvurdering.organisasjonsnummer"].asString().somArbeidsgiverref(),
                beregningsgrunnlag =
                    Beregningsgrunnlag(
                        packet["Avviksvurdering.omregnedeÅrsinntekter"].associate {
                            Arbeidsgiverreferanse(it["organisasjonsnummer"].asString()) to OmregnetÅrsinntekt(it["beløp"].asDouble())
                        },
                    ),
                json = mapper.readValue(packet.toJson()),
            ),
        )
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logg.error("Melding passerte ikke validering i river {}. Se sikkerlogg for mer informasjon", this::class.simpleName)
        sikkerlogg.error("Meldingen passerte ikke validering i river {}. {}", this::class.simpleName, problems.toExtendedReport())
        error("Melding passerte ikke validering i river ${this::class.simpleName}, ${problems.toExtendedReport()}")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }
}
