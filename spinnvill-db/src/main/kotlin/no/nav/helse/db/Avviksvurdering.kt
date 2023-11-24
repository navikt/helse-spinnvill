package no.nav.helse.db

import no.nav.helse.Fødselsnummer
import no.nav.helse.InntektPerMåned
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.dto.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Avviksvurdering {
    private companion object {
        private object Avviksvurderinger : UUIDTable(name = "avviksvurdering") {
            val løpenummer: Column<Long> = long("løpenummer").autoIncrement()
            val fødselsnummer: Column<String> = varchar("fødselsnummer", 11)
            val skjæringstidspunkt: Column<LocalDate> = date("skjæringstidspunkt")
            val opprettet: Column<LocalDateTime> = datetime("opprettet")
        }

        class EnAvviksvurdering(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EnAvviksvurdering>(Avviksvurderinger)

            val beregningsgrunnlag by EttBeregningsgrunnlag optionalReferrersOn Beregningsgrunnlag.avviksvurdering
            val sammenligningsgrunnlag by EttSammenligningsgrunnlag referrersOn Sammenligningsgrunnlag.avviksvurdering

            var fødselsnummer by Avviksvurderinger.fødselsnummer
            var skjæringstidspunkt by Avviksvurderinger.skjæringstidspunkt
            var opprettet by Avviksvurderinger.opprettet
        }

        private object Beregningsgrunnlag : UUIDTable(name = "beregningsgrunnlag") {
            val avviksvurdering = optReference("avviksvurdering_ref", Avviksvurderinger)

            val organisasjonsnummer: Column<String> = varchar("organisasjonsnummer", 9)
            val inntekt: Column<Double> = double("inntekt")
        }

        class EttBeregningsgrunnlag(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EttBeregningsgrunnlag>(Beregningsgrunnlag)

            var avviksvurdering by EnAvviksvurdering optionalReferencedOn Beregningsgrunnlag.avviksvurdering

            var organisasjonsnummer by Beregningsgrunnlag.organisasjonsnummer
            var inntekt by Beregningsgrunnlag.inntekt
        }

        private object Sammenligningsgrunnlag : UUIDTable(name = "sammenligningsgrunnlag") {
            val avviksvurdering = reference("avviksvurdering_ref", Avviksvurderinger)

            val organisasjonsnummer: Column<String> = varchar("organisasjonsnummer", 9)
        }

        class EttSammenligningsgrunnlag(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EttSammenligningsgrunnlag>(Sammenligningsgrunnlag)

            val inntekter by EnMånedsinntekt referrersOn Månedsinntekter.sammenligningsgrunnlag
            var avviksvurdering by EnAvviksvurdering referencedOn Sammenligningsgrunnlag.avviksvurdering

            var organisasjonsnummer by Sammenligningsgrunnlag.organisasjonsnummer
        }

        private object Månedsinntekter : UUIDTable(name = "manedsinntekt") {
            val sammenligningsgrunnlag = reference("sammenligningsgrunnlag_ref", Sammenligningsgrunnlag)

            val inntekt: Column<Double> = double("inntekt")
            val år: Column<Int> = integer("år")
            val måned: Column<Int> = integer("måned")
        }

        class EnMånedsinntekt(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EnMånedsinntekt>(Månedsinntekter)

            var sammenligningsgrunnlag by EttSammenligningsgrunnlag referencedOn Månedsinntekter.sammenligningsgrunnlag

            var inntekt by Månedsinntekter.inntekt
            var år by Månedsinntekter.år
            var måned by Månedsinntekter.måned

            internal val yearMonth: YearMonth get() = YearMonth.of(år, måned)
        }
    }
    fun findLatest(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingDto? {
        return transaction {
            EnAvviksvurdering.find {
                Avviksvurderinger.fødselsnummer eq fødselsnummer.value and (Avviksvurderinger.skjæringstidspunkt eq skjæringstidspunkt)
            }
                .orderBy(Avviksvurderinger.løpenummer to SortOrder.DESC)
                .firstOrNull()
                ?.dto()
        }
    }

    internal fun upsert(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        sammenligningsgrunnlag: AvviksvurderingDto.SammenligningsgrunnlagDto,
        beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto?
    ): AvviksvurderingDto {
        return transaction {
            EnAvviksvurdering.findById(id)?.let {
                update(id, requireNotNull(beregningsgrunnlag))
            } ?: insert(id, fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag, beregningsgrunnlag)
        }
    }

    private fun Transaction.insert(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        sammenligningsgrunnlag: AvviksvurderingDto.SammenligningsgrunnlagDto,
        beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto?
    ): AvviksvurderingDto = this.run {
        val enAvviksvurdering = EnAvviksvurdering.new(id) {
            this.fødselsnummer = fødselsnummer.value
            this.skjæringstidspunkt = skjæringstidspunkt
            this.opprettet = LocalDateTime.now()
        }

        sammenligningsgrunnlag.innrapporterteInntekter.forEach { (organisasjonsnummer, inntekter) ->
            val ettSammenligningsgrunnlag = EttSammenligningsgrunnlag.new {
                this.avviksvurdering = enAvviksvurdering
                this.organisasjonsnummer = organisasjonsnummer.value
            }

            inntekter.forEach { (inntekt, yearMonth) ->
                EnMånedsinntekt.new {
                    this.sammenligningsgrunnlag = ettSammenligningsgrunnlag
                    this.inntekt = inntekt.value
                    this.måned = yearMonth.monthValue
                    this.år = yearMonth.year
                }
            }
        }

        beregningsgrunnlag?.omregnedeÅrsinntekter?.forEach { (organisasjonsnummer, inntekt) ->
            EttBeregningsgrunnlag.new {
                this.organisasjonsnummer = organisasjonsnummer.value
                this.inntekt = inntekt.value
                this.avviksvurdering = enAvviksvurdering
            }
        }
        enAvviksvurdering.dto()
    }

    private fun Transaction.update(id: UUID, beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto): AvviksvurderingDto = this.run {
        val enAvviksvurdering =
            requireNotNull(EnAvviksvurdering.findById(id)) { "Forventer å finne avviksvurdering med id=${id}" }
        beregningsgrunnlag.omregnedeÅrsinntekter.forEach { (organisasjonsnummer, inntekt) ->
            EttBeregningsgrunnlag.new {
                this.organisasjonsnummer = organisasjonsnummer.value
                this.inntekt = inntekt.value
                this.avviksvurdering = enAvviksvurdering
            }
        }
        enAvviksvurdering.dto()
    }

    private fun EnAvviksvurdering.dto(): AvviksvurderingDto {
        return AvviksvurderingDto(
            id = this.id.value,
            fødselsnummer = Fødselsnummer(this.fødselsnummer),
            skjæringstidspunkt = this.skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                innrapporterteInntekter = this.sammenligningsgrunnlag
                    .associate { ettSammenligningsgrunnlag ->
                        Organisasjonsnummer(ettSammenligningsgrunnlag.organisasjonsnummer) to ettSammenligningsgrunnlag.inntekter
                            .map { enMånedsinntekt ->
                                AvviksvurderingDto.MånedligInntektDto(
                                    inntekt = InntektPerMåned(enMånedsinntekt.inntekt),
                                    måned = enMånedsinntekt.yearMonth
                                )
                            }
                    }
            ),
            beregningsgrunnlag = this.beregningsgrunnlag
                .takeUnless { beregningsgrunnlag -> beregningsgrunnlag.empty() }
                ?.let { ettBeregningsgrunnlag ->
                    AvviksvurderingDto.BeregningsgrunnlagDto(
                        omregnedeÅrsinntekter = ettBeregningsgrunnlag
                            .associate { beregningsgrunnlag ->
                                Organisasjonsnummer(beregningsgrunnlag.organisasjonsnummer) to OmregnetÅrsinntekt(beregningsgrunnlag.inntekt)
                            }
                    )
                }
        )
    }
}
