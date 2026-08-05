package no.nav.helse.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import javax.sql.DataSource
import kotlin.test.assertTrue

internal class DatabaseTest {
    @Test
    fun `kan migrere database`() {
        val database = TestDatabase.database()
        database.migrate()

        val success =
            database
                .datasource()
                .let { dataSource -> asSQL("select success from flyway_schema_history").list(dataSource) { it.boolean("success") } }
                .takeIf { it.isNotEmpty() }
                ?.all { succeeeded -> succeeeded } ?: false

        assertTrue(success, "Alle migreringer skal kjøre vellykket")
    }

    private fun asSQL(
        @Language("SQL") sql: String,
        argMap: Map<String, Any?> = emptyMap(),
    ) = queryOf(sql, argMap)

    private fun <T> Query.list(
        dataSource: DataSource,
        mapping: (Row) -> T?,
    ) = sessionOf(dataSource, strict = true).use { session -> session.run(this.map { mapping(it) }.asList) }
}
