package no.nav.helse.avviksvurdering

import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Companion.sum

class Sammenligningsgrunnlag(private val inntekter: List<ArbeidsgiverInntekt>) {

    private val sammenligningsgrunnlag = inntekter.sum()
    internal fun beregnAvvik(beregningsgrunnlag: Beregningsgrunnlag): Avviksprosent {
        return beregningsgrunnlag.beregnAvvik(sammenligningsgrunnlag)
    }

    fun accept(visitor: Visitor) {
        visitor.visitSammenligningsgrunnlag(sammenligningsgrunnlag)
        inntekter.forEach { it.accept(visitor) }
    }

    internal companion object {
        internal val IKKE_INNHENTET = Sammenligningsgrunnlag(emptyList())
    }
}