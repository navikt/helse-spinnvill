package no.nav.helse.modell.avviksvurdering

class Avviksvurdering private constructor(
    private var beregningsgrunnlag: Beregningsgrunnlag
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN


    internal fun håndter(beregningsgrunnlag: Beregningsgrunnlag, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
    }

    internal companion object {
        internal fun nyAvviksvurdering() = Avviksvurdering(Beregningsgrunnlag.INGEN)
    }
}