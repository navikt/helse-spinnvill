package no.nav.helse.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertTrue

internal class DatabaseTest {
    private var port: String

    private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
        withReuse(false)
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

    @Test
    fun `starter en database`() {
        val database = Database(miljøvariabler)

        assertThrows<PSQLException> {
            with(database.datasource()) {
                asSQL("select count(1) from flyway_schema_history").single { it.int(1) }
            }
        }

        database.migrate()

        val antallMigreringer = with(database.datasource()) {
            asSQL("select count(1) from flyway_schema_history").single { it.int(1) }
        } ?: 0

        assertTrue(antallMigreringer > 0, "Det skal ha blitt kjørt mer enn en migration")
    }
}