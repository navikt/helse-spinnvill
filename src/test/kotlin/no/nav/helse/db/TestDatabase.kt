package no.nav.helse.db

import org.testcontainers.containers.PostgreSQLContainer

object TestDatabase {

    private val port: String

    private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
        withReuse(true)
        withLabel("app-navn", "spinnvill")
        start()
        port = firstMappedPort.toString()
        println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
    }

    private val miljøvariabler = mapOf(
        "DATABASE_HOST" to "localhost",
        "DATABASE_PORT" to port,
        "DATABASE_DATABASE" to "test",
        "DATABASE_USERNAME" to postgres.username,
        "DATABASE_PASSWORD" to postgres.password,
    )
    private val dataSourceBuilder = DataSourceBuilder(miljøvariabler)
    internal fun dataSource() = dataSourceBuilder.getDataSource()
    init {
        dataSourceBuilder.migrate()
    }
}