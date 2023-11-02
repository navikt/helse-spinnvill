package no.nav.helse.modell.avviksvurdering

class Avviksvurdering private constructor(
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag
) {

    internal fun avviksvurderingGjortFor(beregningsgrunnlag: Beregningsgrunnlag) =
        this.beregningsgrunnlag == beregningsgrunnlag

    private val avviksprosent get() = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)

    internal fun nyttSammenligningsgrunnlag(sammenligningsgrunnlag: Sammenligningsgrunnlag) =
        Avviksvurdering(this.beregningsgrunnlag, sammenligningsgrunnlag)
    internal companion object {
        internal fun nyAvviksvurdering(beregningsgrunnlag: Beregningsgrunnlag) = Avviksvurdering(beregningsgrunnlag, Sammenligningsgrunnlag.INGEN)
    }
}