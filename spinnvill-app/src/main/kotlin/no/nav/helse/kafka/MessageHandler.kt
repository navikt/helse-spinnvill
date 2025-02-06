package no.nav.helse.kafka

interface MessageHandler {
    fun håndter(message: GodkjenningsbehovMessage)

    fun håndter(message: AvviksvurderingbehovMessage)

    fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage)

}