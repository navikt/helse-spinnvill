package no.nav.helse.kafka

interface MessageHandler {
    fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage)
}