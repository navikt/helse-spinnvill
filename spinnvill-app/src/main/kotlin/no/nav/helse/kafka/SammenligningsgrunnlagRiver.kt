package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Inntektstype
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.avviksvurdering.SammenligningsgrunnlagLøsning
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

internal class SammenligningsgrunnlagRiver(rapidsConnection: RapidsConnection, private val messageHandler: MessageHandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("InntekterForSammenligningsgrunnlag"))
                it.requireKey("@løsning", "fødselsnummer", "InntekterForSammenligningsgrunnlag.skjæringstidspunkt", "InntekterForSammenligningsgrunnlag.avviksvurderingBehovId")
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
        val skjæringstidspunkt = packet["InntekterForSammenligningsgrunnlag.skjæringstidspunkt"].asLocalDate()
        val fødselsnummer = packet["fødselsnummer"].asText().somFnr()
        val avviksvurderingBehovId = packet["InntekterForSammenligningsgrunnlag.avviksvurderingBehovId"].asUUID()
        val sammenligningsgrunnlag = mapSammenligningsgrunnlag(packet["@løsning.InntekterForSammenligningsgrunnlag"])
        logg.info("Leser sammenligningsgrunnlag-løsning")
        sikkerlogg.info("Leser sammenligningsgrunnlag-løsning for {}", kv("fødselsnummer", fødselsnummer.value))
        messageHandler.håndter(
            SammenligningsgrunnlagLøsning(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                avviksvurderingBehovId = avviksvurderingBehovId,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(sammenligningsgrunnlag)
            )
        )
    }

    private fun mapSammenligningsgrunnlag(opplysninger: JsonNode) =
        opplysninger
            .flatMap { måned ->
                måned["inntektsliste"].map { opplysning ->
                    (opplysning as ObjectNode).put("årMåned", måned.path("årMåned").asText())
                }
            }
            .groupBy({ inntekt -> inntekt.arbeidsgiver() }) { inntekt ->
                MånedligInntekt(
                    måned = inntekt["årMåned"].asYearMonth(),
                    inntekt = InntektPerMåned(inntekt["beløp"].asDouble()),
                    inntektstype = inntekt["inntektstype"].asInntektstype(),
                    fordel = if (inntekt.path("fordel").isTextual) Fordel(inntekt["fordel"].asText()) else null,
                    beskrivelse = if (inntekt.path("beskrivelse").isTextual) Beskrivelse(inntekt["beskrivelse"].asText()) else null
                )
            }.map { (arbeidsgiver, inntekter) ->
                ArbeidsgiverInntekt(arbeidsgiver, inntekter)
            }

    private fun JsonNode.asInntektstype() = when (this.asText()) {
        "LOENNSINNTEKT" -> Inntektstype.LØNNSINNTEKT
        "NAERINGSINNTEKT" -> Inntektstype.NÆRINGSINNTEKT
        "PENSJON_ELLER_TRYGD" -> Inntektstype.PENSJON_ELLER_TRYGD
        "YTELSE_FRA_OFFENTLIGE" -> Inntektstype.YTELSE_FRA_OFFENTLIGE
        else -> error("Kunne ikke mappe Inntektstype")
    }

    private fun JsonNode.arbeidsgiver() = when {
        path("orgnummer").isTextual -> path("orgnummer").asText().somArbeidsgiverref()
        path("fødselsnummer").isTextual -> path("fødselsnummer").asText().somArbeidsgiverref()
        else -> error("Mangler arbeidsgiver for inntekt i svar på sammenligningsgrunnlagbehov")
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }
}
