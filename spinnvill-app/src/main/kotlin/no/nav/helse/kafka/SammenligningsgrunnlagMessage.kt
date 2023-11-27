package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import java.time.LocalDate
import java.time.YearMonth

class SammenligningsgrunnlagMessage(packet: JsonMessage) {
    val skjæringstidspunkt: LocalDate = packet["skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val sammenligningsgrunnlag: Map<String, List<Inntekt>> = mapSammenligningsgrunnlag(packet["@løsning.InntekterForSammenligningsgrunnlag"])

    data class Inntekt(
        val beløp: Double,
        val inntektstype: Inntektstype,
        val fordel: String,
        val beskrivelse: String,
        val årMåned: YearMonth
    )

    enum class Inntektstype {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE,
    }

    private fun JsonNode.asInntektstype() = when (this.asText()) {
        "LOENNSINNTEKT" -> Inntektstype.LØNNSINNTEKT
        "NAERINGSINNTEKT" -> Inntektstype.NÆRINGSINNTEKT
        "PENSJON_ELLER_TRYGD" -> Inntektstype.PENSJON_ELLER_TRYGD
        "YTELSE_FRA_OFFENTLIGE" -> Inntektstype.YTELSE_FRA_OFFENTLIGE
        else -> error("Kunne ikke mappe Inntektstype")
    }

    private fun JsonNode.arbeidsgiver() = when {
        path("orgnummer").isTextual -> path("orgnummer").asText()
        path("fødselsnummer").isTextual -> path("fødselsnummer").asText()
        path("aktørId").isTextual -> path("aktørId").asText()
        else -> error("Mangler arbeidsgiver for inntekt i svar på sammenligningsgrunnlagbehov")
    }

    private fun mapSammenligningsgrunnlag(opplysninger: JsonNode) =
        opplysninger
            .flatMap { måned ->
                måned["inntektsliste"].map { opplysning ->
                    (opplysning as ObjectNode).put("årMåned", måned.path("årMåned").asText())
                }
            }
            .groupBy({ inntekt -> inntekt.arbeidsgiver() }) { inntekt ->
                Inntekt(
                    årMåned = inntekt["årMåned"].asYearMonth(),
                    beløp = inntekt["beløp"].asDouble(),
                    inntektstype = inntekt["inntektstype"].asInntektstype(),
                    fordel = if (inntekt.path("fordel").isTextual) inntekt["fordel"].asText() else "",
                    beskrivelse = if (inntekt.path("beskrivelse").isTextual) inntekt["beskrivelse"].asText() else ""
                )
            }
}