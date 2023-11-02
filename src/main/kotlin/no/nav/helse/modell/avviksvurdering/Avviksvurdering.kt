package no.nav.helse.modell.avviksvurdering

class Avviksvurdering(
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag
) {

    internal fun avviksvurderingGjortFor(beregningsgrunnlag: Beregningsgrunnlag) =
        this.beregningsgrunnlag == beregningsgrunnlag

    private val avviksprosent get() = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)

}