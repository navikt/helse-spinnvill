package no.nav.helse.modell.avviksvurdering

import no.nav.helse.modell.avviksvurdering.ArbeidsgiverInntekt.Companion.sum

class Sammenligningsgrunnlag(private val inntekter: List<ArbeidsgiverInntekt>) {

    private val sammenligningsgrunnlag = inntekter.sum()
    internal fun beregnAvvik(beregningsgrunnlag: Beregningsgrunnlag): Avviksprosent {
        return beregningsgrunnlag.beregnAvvik(sammenligningsgrunnlag)
    }

    internal companion object {
        internal val IKKE_INNHENTET = Sammenligningsgrunnlag(emptyList())
    }
}