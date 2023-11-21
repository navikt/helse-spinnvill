package no.nav.helse.db

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

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
    internal fun reset() {
        dataSource().use {
            sessionOf(it).use { session ->
                session.run(queryOf("SELECT truncate_tables()").asExecute)
            }
        }
    }

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
        dataSourceBuilder.migrate()
        dataSource().use { createTruncateFunction(it) }
    }
}
