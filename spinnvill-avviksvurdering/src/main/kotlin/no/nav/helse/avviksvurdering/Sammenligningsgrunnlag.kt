package no.nav.helse.avviksvurdering

import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Companion.sum

class Sammenligningsgrunnlag(private val inntekter: List<ArbeidsgiverInntekt>) {

    private val sammenligningsgrunnlag = inntekter.sum()
    internal fun beregnAvvik(beregningsgrunnlag: Beregningsgrunnlag): Avviksprosent {
        return beregningsgrunnlag.beregnAvvik(sammenligningsgrunnlag)
    }

    internal companion object {
        internal val IKKE_INNHENTET = Sammenligningsgrunnlag(emptyList())
    }
}