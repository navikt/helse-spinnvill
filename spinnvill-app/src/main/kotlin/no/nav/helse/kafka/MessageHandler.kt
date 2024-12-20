package no.nav.helse.kafka

interface MessageHandler {
    fun håndter(message: FastsattISpleis)

    fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage)

}