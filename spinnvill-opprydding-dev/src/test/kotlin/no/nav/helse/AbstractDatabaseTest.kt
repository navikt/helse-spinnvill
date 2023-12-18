package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate

internal abstract class AbstractDatabaseTest {

    protected companion object {

        private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
            withReuse(true)
            withLabel("app-navn", "spinnvill-opprydding")
            start()

            println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
        }

        val dataSource =
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = 5
                connectionTimeout = 500
                initializationFailTimeout = 5000
            })

        private fun createTruncateFunction(dataSource: DataSource) {
            sessionOf(dataSource).use {
                @Language("PostgreSQL")
                val query = """
            CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
            DECLARE
            truncate_statement text;
            BEGIN
                SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' RESTART IDENTITY CASCADE'
                    INTO truncate_statement
                FROM pg_tables
                WHERE schemaname='public'
                AND tablename not in ('flyway_schema_history');

                EXECUTE truncate_statement;
            END;
            $$ LANGUAGE plpgsql;
        """
                it.run(queryOf(query).asExecute)
            }
        }

        init {
            Flyway.configure()
                .dataSource(dataSource)
                .ignoreMigrationPatterns("*:missing")
                .locations("classpath:db/migration")
                .load()
                .migrate()

            createTruncateFunction(dataSource)
        }
    }

    protected fun opprettPerson(
        fødselsnummer: String,
        avviksvurderingId: UUID = UUID.randomUUID(),
        sammenligningsgrunnlagId: UUID = UUID.randomUUID(),
        skjæringstidspunkt: LocalDate = LocalDate.of(2018, 1, 1),
    ) {
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(
                mapOf(
                    "avviksvurdering_id" to avviksvurderingId.toString(),
                    "sammenligninsgrunnlag_id" to sammenligningsgrunnlagId.toString(),
                    "fødselsnummer" to fødselsnummer,
                    "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                )
            )
            .locations("classpath:db/testperson")
            .load()
            .migrate()
    }

    protected fun assertTabellinnhold(booleanExpressionBlock: (actualTabellCount: Int) -> Pair<Boolean, String>) {
        val tabeller = finnTabeller().toMutableList()
        tabeller.remove("flyway_schema_history")
        tabeller.forEach {
            val rowCount = finnRowCount(it)
            if (it in listOf("manedsinntekt")) {
                val (expression1, explanation1) = booleanExpressionBlock(rowCount / 2)
                assertTrue((expression1)) { "$it has $rowCount rows, expected it to be $explanation1" }
            } else {
                val (expression2, explanation2) = booleanExpressionBlock(rowCount)
                assertTrue(expression2) { "$it has $rowCount rows, expected it to be $explanation2" }
            }
        }
    }

    protected fun finnTabeller(): List<String> {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            session.run(queryOf(query).map { it.string("table_name") }.asList)
        }
    }

    private fun finnRowCount(tabellnavn: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM $tabellnavn"
            session.run(queryOf(query).map { it.int(1) }.asSingle) ?: 0
        }
    }

    @BeforeEach
    fun resetDatabase() {
        sessionOf(dataSource).use {
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }
}
