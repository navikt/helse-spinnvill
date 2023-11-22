package no.nav.helse

import no.nav.helse.db.Database
import no.nav.helse.mediator.Mediator
import no.nav.helse.rapids_rivers.RapidApplication

internal fun main() {
    App().start()
}

class App {
    private val rapidsConnection = RapidApplication.create(System.getenv())
    private val database = Database.instance(System.getenv())

    internal fun start() {
        database.migrate()
        rapidsConnection.start()
    }

    init {
        Mediator(rapidsConnection, database)
    }
}