package no.nav.helse.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.Fødselsnummer
import no.nav.helse.dto.AvviksvurderingDto
import java.time.LocalDate

class Database private constructor(env: Map<String, String>) {
    private val dataSourceBuilder = DataSourceBuilder(env)
    private val avviksvurdering = Avviksvurdering()

    init {
        org.jetbrains.exposed.sql.Database.connect(datasource())
    }

    fun migrate() {
        dataSourceBuilder.migrate()
    }

    internal fun datasource(): HikariDataSource = dataSourceBuilder.getDataSource()

    fun finnSisteAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingDto? {
        return avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)
    }

    fun finnAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): List<AvviksvurderingDto> {
        return avviksvurdering.findAll(fødselsnummer, skjæringstidspunkt)
    }

    fun lagreGrunnlagshistorikk(avviksvurderinger: List<AvviksvurderingDto>) {
        avviksvurdering.upsertAll(avviksvurderinger)
    }

    companion object {
        private var instance: Database? = null
        fun instance(env: Map<String, String>): Database {
            return instance ?: synchronized(this) {
                instance ?: Database(env).also { instance = it }
            }
        }
    }
}
