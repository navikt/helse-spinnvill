package no.nav.helse.modell.avviksvurdering

import java.time.LocalDateTime

class Avviksvurderinger {
    private val avviksvurderinger = mutableMapOf<LocalDateTime, Avviksvurdering>()

    private val gjeldende get() = avviksvurderinger.toSortedMap().entries.lastOrNull()?.value

    private fun nyAvviksvurdering(): Avviksvurdering {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering()
        avviksvurderinger[LocalDateTime.now()] = avviksvurdering
        return avviksvurdering
     }

    internal fun håndter(beregningsgrunnlag: Beregningsgrunnlag, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        val avviksvurdering = gjeldende ?: nyAvviksvurdering()
        avviksvurdering.håndter(beregningsgrunnlag, sammenligningsgrunnlag)
    }
}