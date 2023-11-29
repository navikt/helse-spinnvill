package no.nav.helse.helpers

import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import java.time.YearMonth

internal val dummyBeregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)
internal val dummySammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 50000.0)

internal fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) = Beregningsgrunnlag.opprett(
    arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) }
)

internal fun sammenligningsgrunnlag(arbeidsgiverInntekt: Pair<String, Double>): Sammenligningsgrunnlag {
    val (arbeidsgiver, inntekt) = arbeidsgiverInntekt
    return Sammenligningsgrunnlag(
        listOf(
            ArbeidsgiverInntekt(Arbeidsgiverreferanse(arbeidsgiver), List(12) {
                ArbeidsgiverInntekt.MånedligInntekt(
                    inntekt = InntektPerMåned(inntekt),
                    måned = YearMonth.of(2018, it + 1),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                )
            })
        )
    )
}