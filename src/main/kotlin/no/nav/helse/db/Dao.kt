package no.nav.helse.db

import java.time.LocalDate
import javax.sql.DataSource

class Dao(private val dataSource: DataSource) {

    internal fun finnAvviksvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate): String? {
        TODO()
    }
}