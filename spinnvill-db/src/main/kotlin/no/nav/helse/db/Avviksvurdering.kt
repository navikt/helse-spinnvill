package no.nav.helse.db

import no.nav.helse.*
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.dto.AvviksvurderingDto.KildeDto.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Avviksvurdering {
    internal companion object {
        private object Avviksvurderinger : UUIDTable(name = "avviksvurdering") {
            val fødselsnummer: Column<String> = varchar("fødselsnummer", 11)
            val skjæringstidspunkt: Column<LocalDate> = date("skjæringstidspunkt")
            val opprettet: Column<LocalDateTime> = datetime("opprettet")
            val kilde: Column<String> = varchar("kilde", 255)
            val slettet: Column<LocalDateTime?> = datetime("slettet").nullable()
        }

        class EnAvviksvurdering(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EnAvviksvurdering>(Avviksvurderinger)

            val beregningsgrunnlag by EttBeregningsgrunnlag referrersOn Beregningsgrunnlag.avviksvurdering
            val sammenligningsgrunnlag by EttSammenligningsgrunnlag referrersOn Sammenligningsgrunnlag.avviksvurdering

            var fødselsnummer by Avviksvurderinger.fødselsnummer
            var skjæringstidspunkt by Avviksvurderinger.skjæringstidspunkt
            var opprettet by Avviksvurderinger.opprettet
            var kilde by Avviksvurderinger.kilde
        }

        internal object Beregningsgrunnlag : UUIDTable(name = "beregningsgrunnlag") {
            val avviksvurdering = reference("avviksvurdering_ref", Avviksvurderinger)

            val organisasjonsnummer: Column<String> = varchar("organisasjonsnummer", 9)
            val inntekt: Column<Double> = double("inntekt")
        }

        class EttBeregningsgrunnlag(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EttBeregningsgrunnlag>(Beregningsgrunnlag)

            var avviksvurdering by EnAvviksvurdering referencedOn Beregningsgrunnlag.avviksvurdering

            var organisasjonsnummer by Beregningsgrunnlag.organisasjonsnummer
            var inntekt by Beregningsgrunnlag.inntekt
        }

        internal object Sammenligningsgrunnlag : UUIDTable(name = "sammenligningsgrunnlag") {
            val avviksvurdering = reference("avviksvurdering_ref", Avviksvurderinger)

            val arbeidsgiverreferanse: Column<String> = varchar("arbeidsgiverreferanse", 16)
        }

        class EttSammenligningsgrunnlag(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EttSammenligningsgrunnlag>(Sammenligningsgrunnlag)

            val inntekter by EnMånedsinntekt referrersOn Månedsinntekter.sammenligningsgrunnlag
            var avviksvurdering by EnAvviksvurdering referencedOn Sammenligningsgrunnlag.avviksvurdering

            var arbeidsgiverreferanse by Sammenligningsgrunnlag.arbeidsgiverreferanse
        }

        private object Månedsinntekter : UUIDTable(name = "manedsinntekt") {
            val sammenligningsgrunnlag = reference("sammenligningsgrunnlag_ref", Sammenligningsgrunnlag)

            val inntekt: Column<Double> = double("inntekt")
            val år: Column<Int> = integer("år")
            val måned: Column<Int> = integer("måned")
            val inntektstype: Column<String> = varchar("inntektstype", 255)
            val fordel: Column<String?> = varchar("fordel", 255).nullable()
            val beskrivelse: Column<String?> = varchar("beskrivelse", 255).nullable()
        }

        class EnMånedsinntekt(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<EnMånedsinntekt>(Månedsinntekter)

            var sammenligningsgrunnlag by EttSammenligningsgrunnlag referencedOn Månedsinntekter.sammenligningsgrunnlag

            var inntekt by Månedsinntekter.inntekt
            private var år by Månedsinntekter.år
            private var måned by Månedsinntekter.måned
            var inntektstype by Månedsinntekter.inntektstype
            var fordel by Månedsinntekter.fordel
            var beskrivelse by Månedsinntekter.beskrivelse

            internal val yearMonth: YearMonth get() = YearMonth.of(år, måned)
        }
    }

    internal fun findAll(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): List<AvviksvurderingDto> {
        return transaction {
            EnAvviksvurdering.find {
                Avviksvurderinger.fødselsnummer eq fødselsnummer.value and (Avviksvurderinger.skjæringstidspunkt eq skjæringstidspunkt) and Avviksvurderinger.slettet.isNull()
            }
                .orderBy(Avviksvurderinger.opprettet to SortOrder.ASC)
                .map { it.dto() }
        }
    }

    internal fun upsertAll(avviksvurderinger: List<AvviksvurderingDto>): List<AvviksvurderingDto> {
        return transaction {
            avviksvurderinger.map { avviksvurdering ->
                EnAvviksvurdering.findById(avviksvurdering.id)?.let {
                    insertBeregningsgrunnlagIfNotExists(it, avviksvurdering.beregningsgrunnlag)
                } ?: insertAvviksvurdering(
                    avviksvurdering.id,
                    avviksvurdering.fødselsnummer,
                    avviksvurdering.skjæringstidspunkt,
                    avviksvurdering.kilde,
                    avviksvurdering.opprettet,
                    avviksvurdering.sammenligningsgrunnlag,
                    avviksvurdering.beregningsgrunnlag
                )
            }
        }
    }

    private fun Transaction.insertAvviksvurdering(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        kilde: AvviksvurderingDto.KildeDto,
        opprettet: LocalDateTime,
        sammenligningsgrunnlag: AvviksvurderingDto.SammenligningsgrunnlagDto,
        beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto?
    ): AvviksvurderingDto = this.run {
        val enAvviksvurdering = EnAvviksvurdering.new(id) {
            this.fødselsnummer = fødselsnummer.value
            this.skjæringstidspunkt = skjæringstidspunkt
            this.opprettet = opprettet
            this.kilde = kilde.tilDatebase()
        }

        sammenligningsgrunnlag.innrapporterteInntekter.forEach { (arbeidsgiverreferanse, inntekter) ->
            val ettSammenligningsgrunnlag = EttSammenligningsgrunnlag.new {
                this.avviksvurdering = enAvviksvurdering
                this.arbeidsgiverreferanse = arbeidsgiverreferanse.value
            }

            Månedsinntekter.batchInsert(inntekter) {
                this[Månedsinntekter.sammenligningsgrunnlag] = ettSammenligningsgrunnlag.id
                this[Månedsinntekter.inntekt] = it.inntekt.value
                this[Månedsinntekter.måned] = it.måned.monthValue
                this[Månedsinntekter.år] = it.måned.year
                this[Månedsinntekter.fordel] = it.fordel?.value
                this[Månedsinntekter.beskrivelse] = it.beskrivelse?.value
                this[Månedsinntekter.inntektstype] = it.inntektstype.tilDatabase()
            }
        }

        beregningsgrunnlag?.omregnedeÅrsinntekter?.batchInsert(enAvviksvurdering)
        enAvviksvurdering.dto()
    }

    private fun insertBeregningsgrunnlagIfNotExists(
        enAvviksvurdering: EnAvviksvurdering,
        beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto?,
    ): AvviksvurderingDto {
        beregningsgrunnlag?.omregnedeÅrsinntekter?.batchInsert(enAvviksvurdering)
        return enAvviksvurdering.dto()
    }

    private fun Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>.batchInsert(enAvviksvurdering: EnAvviksvurdering) {
        Beregningsgrunnlag.batchInsert(this.toList(), ignore = true) { (organisasjonsnummer, inntekt) ->
            this[Beregningsgrunnlag.organisasjonsnummer] = organisasjonsnummer.value
            this[Beregningsgrunnlag.inntekt] = inntekt.value
            this[Beregningsgrunnlag.avviksvurdering] = enAvviksvurdering.id
        }
    }

    private fun EnAvviksvurdering.dto(): AvviksvurderingDto {
        return AvviksvurderingDto(
            id = this.id.value,
            fødselsnummer = Fødselsnummer(this.fødselsnummer),
            skjæringstidspunkt = this.skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                innrapporterteInntekter = this.sammenligningsgrunnlag
                    .associate { ettSammenligningsgrunnlag ->
                        Arbeidsgiverreferanse(ettSammenligningsgrunnlag.arbeidsgiverreferanse) to ettSammenligningsgrunnlag.inntekter
                            .map { enMånedsinntekt ->
                                AvviksvurderingDto.MånedligInntektDto(
                                    inntekt = InntektPerMåned(enMånedsinntekt.inntekt),
                                    måned = enMånedsinntekt.yearMonth,
                                    fordel = enMånedsinntekt.fordel?.let { Fordel(it) },
                                    beskrivelse = enMånedsinntekt.beskrivelse?.let { Beskrivelse(it) },
                                    inntektstype = enMånedsinntekt.inntektstype.tilInntektstype()
                                )
                            }
                    }
            ),
            kilde = this.kilde.tilKilde(),
            opprettet = opprettet,
            beregningsgrunnlag = this.beregningsgrunnlag
                .takeUnless { beregningsgrunnlag -> beregningsgrunnlag.empty() }
                ?.let { ettBeregningsgrunnlag ->
                    AvviksvurderingDto.BeregningsgrunnlagDto(
                        omregnedeÅrsinntekter = ettBeregningsgrunnlag
                            .associate { beregningsgrunnlag ->
                                Arbeidsgiverreferanse(beregningsgrunnlag.organisasjonsnummer) to OmregnetÅrsinntekt(beregningsgrunnlag.inntekt)
                            }
                    )
                }
        )
    }

    private fun String.tilInntektstype(): AvviksvurderingDto.InntektstypeDto {
        return when (this) {
            "LØNNSINNTEKT" -> AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
            "NÆRINGSINNTEKT" -> AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT
            "PENSJON_ELLER_TRYGD" -> AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD
            "YTELSE_FRA_OFFENTLIGE" -> AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE
            else -> error("Kunne ikke mappe til InntektstypeDto, $this er ikke en gyldig InntektstypeDto")
        }
    }

    private fun String.tilKilde(): AvviksvurderingDto.KildeDto {
        return when (this) {
            "SPINNVILL" -> SPINNVILL
            "SPLEIS" -> SPLEIS
            "INFOTRYGD" -> INFOTRYGD
            else -> error("Kunne ikke mappe til KildeDto, $this er ikke en gyldig KildeDto")
        }
    }

    private fun AvviksvurderingDto.KildeDto.tilDatebase(): String {
        return when (this) {
            SPINNVILL -> "SPINNVILL"
            SPLEIS -> "SPLEIS"
            INFOTRYGD -> "INFOTRYGD"
        }
    }

    private fun AvviksvurderingDto.InntektstypeDto.tilDatabase(): String {
        return when (this) {
            AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT -> "LØNNSINNTEKT"
            AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }
}
