package no.nav.helse

import no.nav.helse.db.Dao
import no.nav.helse.db.DataSourceBuilder
import no.nav.helse.kafka.UtkastTilVedtakRiver
import no.nav.helse.mediator.Mediator
import no.nav.helse.rapids_rivers.RapidApplication

internal fun main() {
    App().start()
}

class App {

    private val rapidsConnection = RapidApplication.create(System.getenv())
    private val datasourceBuilder = DataSourceBuilder(System.getenv())
    private val dao = Dao(datasourceBuilder.getDataSource())
    private val mediator = Mediator(rapidsConnection, dao)

    internal fun start() {
        datasourceBuilder.migrate()
        rapidsConnection.start()
    }

    init {
        UtkastTilVedtakRiver(rapidsConnection, mediator)
    }
}