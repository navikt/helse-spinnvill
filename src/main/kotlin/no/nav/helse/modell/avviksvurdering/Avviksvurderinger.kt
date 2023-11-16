package no.nav.helse.modell.avviksvurdering

import no.nav.helse.modell.avviksvurdering.Avviksvurdering.Companion.sortert

class Avviksvurderinger private constructor(avviksvurderinger: List<Avviksvurdering>) {

    private val avviksvurderinger = avviksvurderinger.toMutableList()

    private val gjeldende get() = avviksvurderinger.sortert().lastOrNull()

    private fun nyAvviksvurdering(): Avviksvurdering {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering()
        avviksvurderinger.add(avviksvurdering)
        return avviksvurdering
     }

    internal fun håndter(beregningsgrunnlag: Beregningsgrunnlag, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        val avviksvurdering = gjeldende ?: nyAvviksvurdering()
        avviksvurdering.håndter(beregningsgrunnlag, sammenligningsgrunnlag)
    }

    internal companion object {
        internal fun ny() = Avviksvurderinger(emptyList())
    }
}