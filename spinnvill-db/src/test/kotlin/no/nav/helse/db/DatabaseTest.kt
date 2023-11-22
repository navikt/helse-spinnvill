package no.nav.helse.db

import no.nav.helse.TestDatabase
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class DatabaseTest {

    @Test
    fun `kan migrere database`() {
        val database = TestDatabase.database()
        database.migrate()

        val success = with(database.datasource()) {
            asSQL("select success from flyway_schema_history").list { it.boolean("success") }
        }.takeIf { it.isNotEmpty() }?.all { succeeeded -> succeeeded } ?: false

        assertTrue(success, "Alle migreringer skal kjøre vellykket")
    }
}