package no.nav.helse.helpers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import java.time.YearMonth

internal val dummyBeregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)
internal val dummySammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 50000.0)

internal fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) = Beregningsgrunnlag(
    arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) }
)

internal fun sammenligningsgrunnlag(vararg arbeidsgiverInntekt: Pair<String, Double>): Sammenligningsgrunnlag {
    return sammenligningsgrunnlag(List(12) { YearMonth.of(2018, it + 1) }, *arbeidsgiverInntekt)
}

internal fun sammenligningsgrunnlag(
    måneder: List<YearMonth>,
    vararg arbeidsgiverInntekt: Pair<String, Double>
): Sammenligningsgrunnlag {
    return Sammenligningsgrunnlag(
        arbeidsgiverInntekt.map { (arbeidsgiver, inntekt) ->
            ArbeidsgiverInntekt(
                Arbeidsgiverreferanse(arbeidsgiver),
                måneder.map {
                    ArbeidsgiverInntekt.MånedligInntekt(
                        inntekt = InntektPerMåned(inntekt),
                        måned = it,
                        fordel = Fordel("En fordel"),
                        beskrivelse = Beskrivelse("En beskrivelse"),
                        inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                    )
                })

        }
    )
}

internal fun Map<String, Any>.toJson() = objectMapper.convertValue(this, JsonNode::class.java)

internal val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
