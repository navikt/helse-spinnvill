package no.nav.helse

import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag

interface KriterieObserver {
    fun avvikVurdert(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double
    ) {}
}