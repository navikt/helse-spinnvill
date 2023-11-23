package no.nav.helse

import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag

interface BehovObserver {
    fun sammenligningsgrunnlag(behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) {}
}