package no.nav.helse

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class Dao(private val dataSource: DataSource) {
    internal fun slett(fødselsnummer: Fødselsnummer) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM avviksvurdering WHERE fødselsnummer = :fodselsnummer
        """.trimIndent()

        sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer.value)).asUpdate)
        }
        slettBehov(fødselsnummer)
    }

    private fun slettBehov(fødselsnummer: Fødselsnummer) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM avviksvurdering_behov WHERE fødselsnummer = :fodselsnummer
        """.trimIndent()

        sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer.value)).asUpdate)
        }
    }
}
