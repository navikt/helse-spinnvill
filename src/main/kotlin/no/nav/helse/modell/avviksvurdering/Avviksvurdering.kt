package no.nav.helse.modell.avviksvurdering

import no.nav.helse.modell.KriterieObserver
import java.time.LocalDateTime

class Avviksvurdering private constructor(
    private val opprettet: LocalDateTime,
    private var beregningsgrunnlag: Beregningsgrunnlag
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN

    private val observers = mutableListOf<KriterieObserver>()

    internal fun register(vararg observers: KriterieObserver) {
        this.observers.addAll(observers)
    }

    internal fun håndter(beregningsgrunnlag: Beregningsgrunnlag, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        observers.forEach {
            it.avvikVurdert(avviksprosent.harAkseptabeltAvvik(), avviksprosent.avrundetTilToDesimaler())
        }
    }

    internal companion object {
        internal fun nyAvviksvurdering() = Avviksvurdering(LocalDateTime.now(), Beregningsgrunnlag.INGEN)
        internal fun Iterable<Avviksvurdering>.sortert() = sortedBy { it.opprettet }
    }
}