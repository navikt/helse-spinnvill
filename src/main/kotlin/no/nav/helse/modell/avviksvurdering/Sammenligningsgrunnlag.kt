package no.nav.helse.modell.avviksvurdering

class Sammenligningsgrunnlag(private val inntekter: List<Double>) {

    private val sammenligningsgrunnlag = inntekter.sum()
    internal fun beregnAvvik(beregningsgrunnlag: Beregningsgrunnlag): Avviksprosent {
        return beregningsgrunnlag.beregnAvvik(sammenligningsgrunnlag)
    }

    internal companion object {
        internal val INGEN = Sammenligningsgrunnlag(emptyList())
    }

}