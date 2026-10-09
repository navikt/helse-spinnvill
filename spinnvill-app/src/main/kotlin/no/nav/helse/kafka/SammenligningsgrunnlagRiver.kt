package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Inntektstype
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.avviksvurdering.SammenligningsgrunnlagLøsning
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

internal class SammenligningsgrunnlagRiver(
    rapidsConnection: RapidsConnection,
    private val messageHandler: MessageHandler,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAll("@behov", listOf("InntekterForSammenligningsgrunnlag"))
                    it.requireKey("@løsning")
                    it.requireValue("@final", true)
                }
                validate {
                    it.requireKey("fødselsnummer", "InntekterForSammenligningsgrunnlag.skjæringstidspunkt", "InntekterForSammenligningsgrunnlag.avviksvurderingBehovId")
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

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val skjæringstidspunkt = packet["InntekterForSammenligningsgrunnlag.skjæringstidspunkt"].asLocalDate()
        val fødselsnummer = packet["fødselsnummer"].asString().somFnr()
        val avviksvurderingBehovId = packet["InntekterForSammenligningsgrunnlag.avviksvurderingBehovId"].asUUID()
        val sammenligningsgrunnlag = mapSammenligningsgrunnlag(packet["@løsning.InntekterForSammenligningsgrunnlag"])
        logg.info("Leser sammenligningsgrunnlag-løsning")
        sikkerlogg.info("Leser sammenligningsgrunnlag-løsning for {}", kv("fødselsnummer", fødselsnummer.value))
        messageHandler.håndter(
            SammenligningsgrunnlagLøsning(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                avviksvurderingBehovId = avviksvurderingBehovId,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(sammenligningsgrunnlag),
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

    private fun mapSammenligningsgrunnlag(opplysninger: JsonNode) =
        opplysninger
            .flatMap { måned ->
                måned["inntektsliste"].values().map { opplysning ->
                    (opplysning as ObjectNode).put("årMåned", måned.path("årMåned").asString())
                }
            }.groupBy({ inntekt -> inntekt.arbeidsgiver() }) { inntekt ->
                MånedligInntekt(
                    måned = inntekt["årMåned"].asYearMonth(),
                    inntekt = InntektPerMåned(inntekt["beløp"].asDouble()),
                    inntektstype = inntekt["inntektstype"].asInntektstype(),
                    fordel = if (inntekt.path("fordel").isString) Fordel(inntekt["fordel"].asString()) else null,
                    beskrivelse = if (inntekt.path("beskrivelse").isString) Beskrivelse(inntekt["beskrivelse"].asString()) else null,
                )
            }.map { (arbeidsgiver, inntekter) ->
                ArbeidsgiverInntekt(arbeidsgiver, inntekter)
            }

    private fun JsonNode.asInntektstype() =
        when (this.asString()) {
            "LOENNSINNTEKT" -> Inntektstype.LØNNSINNTEKT
            "NAERINGSINNTEKT" -> Inntektstype.NÆRINGSINNTEKT
            "PENSJON_ELLER_TRYGD" -> Inntektstype.PENSJON_ELLER_TRYGD
            "YTELSE_FRA_OFFENTLIGE" -> Inntektstype.YTELSE_FRA_OFFENTLIGE
            else -> error("Kunne ikke mappe Inntektstype")
        }

    private fun JsonNode.arbeidsgiver() =
        when {
            path("orgnummer").isString -> path("orgnummer").asString().somArbeidsgiverref()
            path("fødselsnummer").isString -> path("fødselsnummer").asString().somArbeidsgiverref()
            else -> error("Mangler arbeidsgiver for inntekt i svar på sammenligningsgrunnlagbehov")
        }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }
}
