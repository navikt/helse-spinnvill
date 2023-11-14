package no.nav.helse.modell

interface BehovObserver {
    fun sammenligningsgrunnlag(behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) {}
}