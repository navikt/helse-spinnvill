package no.nav.helse.modell.avviksvurdering

import java.time.LocalDateTime

class Avviksvurdering private constructor(
    private val opprettet: LocalDateTime,
    private var beregningsgrunnlag: Beregningsgrunnlag
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN


    internal fun håndter(beregningsgrunnlag: Beregningsgrunnlag, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
    }

    internal companion object {
        internal fun nyAvviksvurdering() = Avviksvurdering(LocalDateTime.now(), Beregningsgrunnlag.INGEN)
        internal fun Iterable<Avviksvurdering>.sortert() = sortedBy { it.opprettet }
    }
}