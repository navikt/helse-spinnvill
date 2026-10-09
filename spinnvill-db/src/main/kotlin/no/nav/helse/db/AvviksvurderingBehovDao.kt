package no.nav.helse.db

import no.nav.helse.Fødselsnummer
import no.nav.helse.dto.AvviksvurderingBehovDto
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.convertValue
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class AvviksvurderingBehovDao {
    internal companion object {
        val mapper = jacksonObjectMapper()

        private object AvviksvurderingBehov : IdTable<UUID>(name = "avviksvurdering_behov") {
            override val id: Column<EntityID<UUID>> = javaUUID("behov_id").entityId()
            val fødselsnummer: Column<String> = varchar("fødselsnummer", 11)
            val skjæringstidspunkt: Column<LocalDate> = date("skjæringstidspunkt")
            val opprettet: Column<LocalDateTime> = datetime("opprettet")
            val løst: Column<LocalDateTime?> = datetime("løst").nullable()
            val json: Column<JsonNode> = json("json", { mapper.writeValueAsString(it) }, { mapper.readTree(it) })

            override val primaryKey = PrimaryKey(id)
        }

        class EtAvviksvurderingBehov(
            id: EntityID<UUID>,
        ) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EtAvviksvurderingBehov>(AvviksvurderingBehov)

            var fødselsnummer by AvviksvurderingBehov.fødselsnummer
            var skjæringstidspunkt by AvviksvurderingBehov.skjæringstidspunkt
            var opprettet by AvviksvurderingBehov.opprettet
            var løst by AvviksvurderingBehov.løst
            var json by AvviksvurderingBehov.json
        }
    }

    internal fun findUløst(
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
    ): AvviksvurderingBehovDto? =
        transaction {
            EtAvviksvurderingBehov
                .find {
                    AvviksvurderingBehov.fødselsnummer eq fødselsnummer.value and (AvviksvurderingBehov.skjæringstidspunkt eq skjæringstidspunkt) and AvviksvurderingBehov.løst.isNull()
                }.firstOrNull()
                ?.dto()
        }

    internal fun lagre(avviksvurderingBehovDto: AvviksvurderingBehovDto) =
        transaction {
            EtAvviksvurderingBehov.findByIdAndUpdate(avviksvurderingBehovDto.id) {
                it.løst = avviksvurderingBehovDto.løst
            } ?: EtAvviksvurderingBehov.new(avviksvurderingBehovDto.id) {
                fødselsnummer = avviksvurderingBehovDto.fødselsnummer
                skjæringstidspunkt = avviksvurderingBehovDto.skjæringstidspunkt
                opprettet = avviksvurderingBehovDto.opprettet
                løst = null
                json = mapper.valueToTree(avviksvurderingBehovDto.json)
            }
        }

    internal fun slett(behovId: UUID) {
        transaction {
            EtAvviksvurderingBehov.findById(behovId)?.delete()
        }
    }

    private fun EtAvviksvurderingBehov.dto(): AvviksvurderingBehovDto =
        AvviksvurderingBehovDto(
            id = this.id.value,
            fødselsnummer = this.fødselsnummer,
            skjæringstidspunkt = this.skjæringstidspunkt,
            opprettet = opprettet,
            løst = løst,
            json = mapper.convertValue(json),
        )
}
