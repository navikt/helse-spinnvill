package no.nav.helse

import no.nav.helse.db.DataSourceBuilder
import no.nav.helse.kafka.UtkastTilVedtakRiver
import no.nav.helse.rapids_rivers.RapidApplication

internal fun main() {
    App().start()
}

class App {

    private val rapidsConnection = RapidApplication.create(System.getenv())
    private val datasourceBuilder = DataSourceBuilder(System.getenv())

    init {
        UtkastTilVedtakRiver(rapidsConnection)
    }

    internal fun start() {
        datasourceBuilder.migrate()
        rapidsConnection.start()
    }

}