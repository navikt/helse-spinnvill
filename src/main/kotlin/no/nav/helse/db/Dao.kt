package no.nav.helse.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class Dao(private val dataSource: DataSource) {

    internal fun finnAvviksvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate): String? {
        return with(dataSource) {
            asSQL(
                """
                SELECT json FROM avviksvurdering WHERE fødselsnummer = :fodselsnummer AND skjæringstidspunkt = :skjaeringspunkt
            """.trimIndent(),
                mapOf(
                    "fodselsnummer" to fødselsnummer,
                    "skjaeringstidspunkt" to skjæringstidspunkt
                )
            ).single { it.string("json") }
        }
    }
}

fun asSQL(@Language("SQL") sql: String, argMap: Map<String, Any?> = emptyMap()) = queryOf(sql, argMap)
fun asSQL(@Language("SQL") sql: String, vararg params: Any?) = queryOf(sql, *params)

context (DataSource)
fun <T> Query.single(mapping: (Row) -> T?) =
    sessionOf(this@DataSource, strict = true).use { session -> session.run(this.map { mapping(it) }.asSingle) }
