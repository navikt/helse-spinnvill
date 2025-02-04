package no.nav.helse.avviksvurdering

import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Companion.sum

class Sammenligningsgrunnlag(val inntekter: List<ArbeidsgiverInntekt>) {

    val totaltInnrapportertÅrsinntekt = inntekter.sum()
    internal fun beregnAvvik(beregningsgrunnlag: Beregningsgrunnlag): Avviksprosent {
        return beregningsgrunnlag.beregnAvvik(totaltInnrapportertÅrsinntekt)
    }
}
