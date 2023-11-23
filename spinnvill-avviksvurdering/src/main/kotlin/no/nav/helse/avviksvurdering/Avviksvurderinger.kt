package no.nav.helse.avviksvurdering

import no.nav.helse.avviksvurdering.Avviksvurdering.Companion.sortert

class Avviksvurderinger private constructor(avviksvurderinger: List<Avviksvurdering>) {

    private val avviksvurderinger = avviksvurderinger.toMutableList()

    private val gjeldende get() = avviksvurderinger.sortert().lastOrNull()

    private fun nyAvviksvurdering(sammenligningsgrunnlag: Sammenligningsgrunnlag): Avviksvurdering {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering(sammenligningsgrunnlag)
        avviksvurderinger.add(avviksvurdering)
        return avviksvurdering
     }

    internal fun håndter(beregningsgrunnlag: Beregningsgrunnlag, sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        val avviksvurdering = gjeldende ?: nyAvviksvurdering(sammenligningsgrunnlag)
        avviksvurdering.håndter(beregningsgrunnlag)
    }

    internal companion object {
        internal fun ny() = Avviksvurderinger(emptyList())
    }
}