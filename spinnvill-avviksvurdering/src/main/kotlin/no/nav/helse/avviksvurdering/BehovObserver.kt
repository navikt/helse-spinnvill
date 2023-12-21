package no.nav.helse.avviksvurdering

interface BehovObserver {
    fun sammenligningsgrunnlag(behov: BehovForSammenligningsgrunnlag) {}
}