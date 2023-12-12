package no.nav.helse.mediator.producer

import no.nav.helse.kafka.UtkastTilVedtakMessage

internal class UtkastTilVedtakProducer(
    private val utkastTilVedtakMessage: UtkastTilVedtakMessage
) : Producer {
    private val utkastTilVedtak = mutableListOf<UtkastTilVedtakMessage>()
    internal fun håndter() {
        utkastTilVedtak.add(utkastTilVedtakMessage)
    }

    override fun ferdigstill(): List<Message> {
        return utkastTilVedtak.map {
            Message.Behov(
                setOf("Godkjenning"),
                it.finalize()
            )
        }
    }

}