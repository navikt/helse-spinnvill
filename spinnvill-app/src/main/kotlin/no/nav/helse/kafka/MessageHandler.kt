package no.nav.helse.kafka

import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.SammenligningsgrunnlagLøsning

interface MessageHandler {

    fun håndter(behov: AvviksvurderingBehov)

    fun håndter(løsning: SammenligningsgrunnlagLøsning)
}
