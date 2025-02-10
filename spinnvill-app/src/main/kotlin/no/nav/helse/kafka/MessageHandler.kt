package no.nav.helse.kafka

import no.nav.helse.avviksvurdering.AvviksvurderingBehov

interface MessageHandler {
    fun håndter(message: GodkjenningsbehovMessage)

    fun håndter(behov: AvviksvurderingBehov)

    fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage)

}
