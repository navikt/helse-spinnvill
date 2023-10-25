package no.nav.helse

import no.nav.helse.rapids_rivers.RapidApplication

internal fun main() {
    App().start()
}

class App {

    private val rapidsConnection = RapidApplication.create(System.getenv())

    init {
        UtkastTilVedtakRiver(rapidsConnection)
    }

    internal fun start() {
        rapidsConnection.start()
    }

}