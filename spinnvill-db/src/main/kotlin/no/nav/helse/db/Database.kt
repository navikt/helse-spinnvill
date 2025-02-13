package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.*
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.dto.AvviksvurderingBehovDto
import no.nav.helse.dto.AvviksvurderingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Database private constructor(env: Map<String, String>) {
    private val dataSourceBuilder = DataSourceBuilder(env)
    private val avviksvurdering = Avviksvurdering()
    private val avviksvurderingBehovDao = AvviksvurderingBehovDao()

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

    fun finnUbehandledeAvviksvurderingBehov(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingBehov? {
        return avviksvurderingBehovDao.findUløst(fødselsnummer, skjæringstidspunkt)?.let { dto ->
            val jsonNode = jacksonObjectMapper().convertValue<JsonNode>(dto.json)
            AvviksvurderingBehov.fraLagring(
                behovId = dto.id,
                vilkårsgrunnlagId = jsonNode["Avviksvurdering"].get("vilkårsgrunnlagId").asUUID(),
                skjæringstidspunkt = dto.skjæringstidspunkt,
                fødselsnummer = dto.fødselsnummer.somFnr(),
                vedtaksperiodeId = jsonNode["vedtaksperiodeId"].asUUID(),
                organisasjonsnummer = jsonNode["organisasjonsnummer"].asText().somArbeidsgiverref(),
                løst = dto.løst != null,
                beregningsgrunnlag = Beregningsgrunnlag.opprett(
                    jsonNode["Avviksvurdering"].get("omregnedeÅrsinntekter").associate {
                        Arbeidsgiverreferanse(it["organisasjonsnummer"].asText()) to OmregnetÅrsinntekt(it["beløp"].asDouble())
                    }
                ),
                json = dto.json,
            )
        }
    }

    fun lagreAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov) {
        val dto = AvviksvurderingBehovDto(
            avviksvurderingBehov.behovId,
            fødselsnummer = avviksvurderingBehov.fødselsnummer.value,
            skjæringstidspunkt = avviksvurderingBehov.skjæringstidspunkt,
            opprettet = LocalDateTime.now(),
            løst = if (avviksvurderingBehov.erLøst()) LocalDateTime.now() else null,
            json = avviksvurderingBehov.json
        )
        avviksvurderingBehovDao.lagre(dto)
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

        private fun JsonNode.asUUID(): UUID = UUID.fromString(this.asText())
    }
}
