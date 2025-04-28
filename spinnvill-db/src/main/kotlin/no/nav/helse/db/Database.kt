package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.dto.AvviksvurderingBehovDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class PgDatabase private constructor(env: Map<String, String>): Database {
    private val dataSourceBuilder = DataSourceBuilder(env)
    private val avviksvurderingRepository = AvviksvurderingRepository()
    private val avviksvurderingBehovDao = AvviksvurderingBehovDao()

    init {
        org.jetbrains.exposed.sql.Database.connect(datasource())
    }

    override fun migrate() {
        dataSourceBuilder.migrate()
    }

    override fun datasource(): HikariDataSource = dataSourceBuilder.getDataSource()

    override fun finnUbehandletAvviksvurderingBehov(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingBehov? {
        return avviksvurderingBehovDao.findUløst(fødselsnummer, skjæringstidspunkt)?.let { dto ->
            val jsonNode = jacksonObjectMapper().convertValue<JsonNode>(dto.json)
            AvviksvurderingBehov.fraLagring(
                behovId = dto.id,
                vilkårsgrunnlagId = jsonNode["Avviksvurdering"].get("vilkårsgrunnlagId").asUUID(),
                skjæringstidspunkt = dto.skjæringstidspunkt,
                fødselsnummer = dto.fødselsnummer.somFnr(),
                vedtaksperiodeId = jsonNode["Avviksvurdering"].get("vedtaksperiodeId").asUUID(),
                organisasjonsnummer = jsonNode["Avviksvurdering"].get("organisasjonsnummer").asText().somArbeidsgiverref(),
                løst = dto.løst != null,
                beregningsgrunnlag = Beregningsgrunnlag(
                    jsonNode["Avviksvurdering"].get("omregnedeÅrsinntekter").associate {
                        Arbeidsgiverreferanse(it["organisasjonsnummer"].asText()) to OmregnetÅrsinntekt(it["beløp"].asDouble())
                    }
                ),
                json = dto.json,
                opprettet = dto.opprettet,
            )
        }
    }

    override fun lagreAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov) {
        val dto = AvviksvurderingBehovDto(
            avviksvurderingBehov.behovId,
            fødselsnummer = avviksvurderingBehov.fødselsnummer.value,
            skjæringstidspunkt = avviksvurderingBehov.skjæringstidspunkt,
            opprettet = avviksvurderingBehov.opprettet,
            løst = if (avviksvurderingBehov.erLøst()) LocalDateTime.now() else null,
            json = avviksvurderingBehov.json
        )
        avviksvurderingBehovDao.lagre(dto)
    }

    override fun slettAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov) {
        avviksvurderingBehovDao.slett(avviksvurderingBehov.behovId)
    }

    override fun finnAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurderingsgrunnlag? {
        return avviksvurderingRepository.findLatest(fødselsnummer, skjæringstidspunkt)
    }

    override fun lagreAvviksvurderinggrunnlag(grunnlag: Avviksvurderingsgrunnlag) {
        avviksvurderingRepository.insertOne(grunnlag)
    }

    companion object {
        private var instance: Database? = null
        fun instance(env: Map<String, String>): Database {
            return instance ?: synchronized(this) {
                instance ?: PgDatabase(env).also { instance = it }
            }
        }

        private fun JsonNode.asUUID(): UUID = UUID.fromString(this.asText())
    }
}

interface Database {
    fun datasource(): HikariDataSource
    fun migrate()

    fun finnUbehandletAvviksvurderingBehov(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingBehov?

    fun lagreAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov)

    fun finnAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurderingsgrunnlag?

    fun lagreAvviksvurderinggrunnlag(grunnlag: Avviksvurderingsgrunnlag)
    fun slettAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov)
}
