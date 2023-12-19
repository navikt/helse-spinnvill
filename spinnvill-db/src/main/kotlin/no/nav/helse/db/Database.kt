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

    fun lagreAvviksvurdering(avviksvurderingDto: AvviksvurderingDto): AvviksvurderingDto {
        return avviksvurdering.upsert(
            id = avviksvurderingDto.id,
            fødselsnummer = avviksvurderingDto.fødselsnummer,
            skjæringstidspunkt = avviksvurderingDto.skjæringstidspunkt,
            kilde = avviksvurderingDto.kilde,
            sammenligningsgrunnlag = avviksvurderingDto.sammenligningsgrunnlag,
            beregningsgrunnlag = avviksvurderingDto.beregningsgrunnlag
        )
    }

    fun finnSisteAvviksvurdering(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingDto? {
        return avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)
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