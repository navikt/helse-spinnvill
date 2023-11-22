package no.nav.helse.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

internal class Dao(private val dataSource: DataSource) {

    internal fun finnAvviksvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate): String? {
        return with(dataSource) {
            asSQL(
                """
                SELECT json FROM avviksvurdering 
                WHERE fødselsnummer = :fodselsnummer AND skjæringstidspunkt = :skjaeringstidspunkt 
                ORDER BY opprettet DESC LIMIT 1
            """.trimIndent(),
                mapOf(
                    "fodselsnummer" to fødselsnummer,
                    "skjaeringstidspunkt" to skjæringstidspunkt
                )
            ).single { it.string("json") }
        }
    }

    fun opprettAvviksvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate, json: String) {
        with(dataSource) {
            asSQL(
                """
                INSERT INTO avviksvurdering (fødselsnummer, skjæringstidspunkt, opprettet, json) 
                VALUES (:fodselsnummer, :skjaeringstidspunkt, :opprettet, :json::json)
            """.trimIndent(),
                mapOf(
                    "fodselsnummer" to fødselsnummer,
                    "skjaeringstidspunkt" to skjæringstidspunkt,
                    "opprettet" to LocalDateTime.now(),
                    "json" to json
                )
            ).update()
        }
    }
}

internal fun asSQL(@Language("SQL") sql: String, argMap: Map<String, Any?> = emptyMap()) = queryOf(sql, argMap)
internal fun asSQL(@Language("SQL") sql: String, vararg params: Any?) = queryOf(sql, *params)

context (DataSource)
internal fun Query.update() = sessionOf(this@DataSource).use { session -> session.run(this.asUpdate) }

context (DataSource)
internal fun <T> Query.single(mapping: (Row) -> T?) =
    sessionOf(this@DataSource, strict = true).use { session -> session.run(this.map { mapping(it) }.asSingle) }

context (DataSource)
internal fun <T> Query.list(mapping: (Row) -> T?) =
    sessionOf(this@DataSource, strict = true).use { session -> session.run(this.map { mapping(it) }.asList) }

