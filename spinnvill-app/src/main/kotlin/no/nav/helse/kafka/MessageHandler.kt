package no.nav.helse.kafka

interface MessageHandler {
    fun håndter(message: GodkjenningsbehovMessage)

    fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage)

}