package no.nav.helse.avviksvurdering

import no.nav.helse.KriterieObserver

class Avviksvurdering(
    private var beregningsgrunnlag: Beregningsgrunnlag,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN

    private val observers = mutableListOf<KriterieObserver>()

    internal fun register(vararg observers: KriterieObserver) {
        this.observers.addAll(observers)
    }

    fun håndter(beregningsgrunnlag: Beregningsgrunnlag) {
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        observers.forEach {
            it.avvikVurdert(avviksprosent.harAkseptabeltAvvik(), avviksprosent.avrundetTilToDesimaler())
        }
    }

    internal companion object {
        internal fun nyAvviksvurdering(sammenligningsgrunnlag: Sammenligningsgrunnlag) = Avviksvurdering(Beregningsgrunnlag.INGEN, sammenligningsgrunnlag)
    }
}