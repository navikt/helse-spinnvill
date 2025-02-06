package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Fødselsnummer
import no.nav.helse.dto.AvviksvurderingBehovDto
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class AvviksvurderingBehovDao {
    internal companion object {
    val mapper = jacksonObjectMapper()
        private object AvviksvurderingBehov : IdTable<UUID>(name = "avviksvurdering_behov") {
            override val id: Column<EntityID<UUID>> = uuid("behov_id").entityId()
            val fødselsnummer: Column<String> = varchar("fødselsnummer", 11)
            val skjæringstidspunkt: Column<LocalDate> = date("skjæringstidspunkt")
            val opprettet: Column<LocalDateTime> = datetime("opprettet")
            val løst: Column<LocalDateTime?> = datetime("løst").nullable()
            val json: Column<JsonNode> = json("json",  { mapper.writeValueAsString(it) }, { mapper.readTree(it) })

            override val primaryKey = PrimaryKey(id)
        }

        class EtAvviksvurderingBehov(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EtAvviksvurderingBehov>(AvviksvurderingBehov)

            var fødselsnummer by AvviksvurderingBehov.fødselsnummer
            var skjæringstidspunkt by AvviksvurderingBehov.skjæringstidspunkt
            var opprettet by AvviksvurderingBehov.opprettet
            var løst by AvviksvurderingBehov.løst
            var json  by AvviksvurderingBehov.json
        }
    }

    internal fun findUløst(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingBehovDto? {
        return transaction {
            EtAvviksvurderingBehov.find {
                AvviksvurderingBehov.fødselsnummer eq fødselsnummer.value and (AvviksvurderingBehov.skjæringstidspunkt eq skjæringstidspunkt) and AvviksvurderingBehov.løst.isNull()
            }
                .firstOrNull()
                ?.dto()
        }
    }

    internal fun lagre(avviksvurderingBehovDto: AvviksvurderingBehovDto): AvviksvurderingBehovDto {
        return transaction {
            val etAvviksvurderingBehov = EtAvviksvurderingBehov.new(avviksvurderingBehovDto.id) {
                fødselsnummer = avviksvurderingBehovDto.fødselsnummer
                skjæringstidspunkt = avviksvurderingBehovDto.skjæringstidspunkt
                opprettet = avviksvurderingBehovDto.opprettet
                løst = avviksvurderingBehovDto.løst ?: løst
                json = mapper.valueToTree(avviksvurderingBehovDto.json)
            }
            etAvviksvurderingBehov.dto()
        }
    }


    private fun EtAvviksvurderingBehov.dto(): AvviksvurderingBehovDto {
        return AvviksvurderingBehovDto(
            id = this.id.value,
            fødselsnummer = this.fødselsnummer,
            skjæringstidspunkt = this.skjæringstidspunkt,
            opprettet = opprettet,
            løst = løst,
            json =  mapper.convertValue(json)
        )
    }
}

