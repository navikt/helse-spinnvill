package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import java.time.LocalDate
import java.time.YearMonth

class SammenligningsgrunnlagMessage(packet: JsonMessage) {
    val skjæringstidspunkt: LocalDate = packet["skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val sammenligningsgrunnlag: Map<String, List<Inntekt>> = packet["@løsning.InntekterForSammenligningsgrunnlag"].flatMap { inntekterJson ->
        val årMåned = inntekterJson["årMåned"].asYearMonth()
        inntekterJson["inntektsliste"].map { inntektJson ->
            inntektJson["orgnummer"].asText() to Inntekt(
                beløp = inntektJson["beløp"].asDouble(),
                inntektstype = inntektJson["inntektstype"].asText(),
                fordel = inntektJson["fordel"].asText(),
                beskrivelse = inntektJson["beskrivelse"].asText(),
                årMåned = årMåned
            )
        }
    }.groupBy({ (orgnummer, _) -> orgnummer }) { (_, inntekter) -> inntekter }

    data class Inntekt(
        val beløp: Double,
        val inntektstype: String,
        val fordel: String,
        val beskrivelse: String,
        val årMåned: YearMonth
    )
}