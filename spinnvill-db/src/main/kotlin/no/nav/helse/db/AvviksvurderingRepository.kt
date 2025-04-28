package no.nav.helse.db

import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
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

internal class AvviksvurderingRepository {
    internal companion object {
        private object AvviksvurderingTable : UUIDTable(name = "avviksvurdering") {
            val fødselsnummer: Column<String> = varchar("fødselsnummer", 11)
            val skjæringstidspunkt: Column<LocalDate> = date("skjæringstidspunkt")
            val opprettet: Column<LocalDateTime> = datetime("opprettet")
            val kilde: Column<String> = varchar("kilde", 255)
            val slettet: Column<LocalDateTime?> = datetime("slettet").nullable()
        }

        class AvviksvurderingRow(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<AvviksvurderingRow>(AvviksvurderingTable)

            val beregningsgrunnlag by BeregningsgrunnlagRow referrersOn BeregningsgrunnlagTable.avviksvurdering
            val sammenligningsgrunnlag by SammenligningsgrunnlagRow referrersOn SammenligningsgrunnlagTable.avviksvurdering

            var fødselsnummer by AvviksvurderingTable.fødselsnummer
            var skjæringstidspunkt by AvviksvurderingTable.skjæringstidspunkt
            var opprettet by AvviksvurderingTable.opprettet
            var kilde by AvviksvurderingTable.kilde
        }

        internal object BeregningsgrunnlagTable : UUIDTable(name = "beregningsgrunnlag") {
            val avviksvurdering = reference("avviksvurdering_ref", AvviksvurderingTable)

            val organisasjonsnummer: Column<String> = varchar("organisasjonsnummer", 9)
            val inntekt: Column<Double> = double("inntekt")
        }

        class BeregningsgrunnlagRow(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<BeregningsgrunnlagRow>(BeregningsgrunnlagTable)

            var avviksvurdering by AvviksvurderingRow referencedOn BeregningsgrunnlagTable.avviksvurdering

            var organisasjonsnummer by BeregningsgrunnlagTable.organisasjonsnummer
            var inntekt by BeregningsgrunnlagTable.inntekt
        }

        internal object SammenligningsgrunnlagTable : UUIDTable(name = "sammenligningsgrunnlag") {
            val avviksvurdering = reference("avviksvurdering_ref", AvviksvurderingTable)

            val arbeidsgiverreferanse: Column<String> = varchar("arbeidsgiverreferanse", 16)
        }

        class SammenligningsgrunnlagRow(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<SammenligningsgrunnlagRow>(SammenligningsgrunnlagTable)

            val inntekter by MånedsinntektRow referrersOn MånedsinntektTable.sammenligningsgrunnlag
            var avviksvurdering by AvviksvurderingRow referencedOn SammenligningsgrunnlagTable.avviksvurdering

            var arbeidsgiverreferanse by SammenligningsgrunnlagTable.arbeidsgiverreferanse
        }

        private object MånedsinntektTable : UUIDTable(name = "manedsinntekt") {
            val sammenligningsgrunnlag = reference("sammenligningsgrunnlag_ref", SammenligningsgrunnlagTable)

            val inntekt: Column<Double> = double("inntekt")
            val år: Column<Int> = integer("år")
            val måned: Column<Int> = integer("måned")
            val inntektstype: Column<String> = varchar("inntektstype", 255)
            val fordel: Column<String?> = varchar("fordel", 255).nullable()
            val beskrivelse: Column<String?> = varchar("beskrivelse", 255).nullable()
        }

        class MånedsinntektRow(id: EntityID<UUID>) : UUIDEntity(id) {
            companion object : UUIDEntityClass<MånedsinntektRow>(MånedsinntektTable)

            var sammenligningsgrunnlag by SammenligningsgrunnlagRow referencedOn MånedsinntektTable.sammenligningsgrunnlag

            var inntekt by MånedsinntektTable.inntekt
            private var år by MånedsinntektTable.år
            private var måned by MånedsinntektTable.måned
            var inntektstype by MånedsinntektTable.inntektstype
            var fordel by MånedsinntektTable.fordel
            var beskrivelse by MånedsinntektTable.beskrivelse

            internal val yearMonth: YearMonth get() = YearMonth.of(år, måned)
        }
    }

    internal fun findLatest(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurderingsgrunnlag? {
        return transaction {
            AvviksvurderingRow.find {
                AvviksvurderingTable.fødselsnummer eq fødselsnummer.value and (AvviksvurderingTable.skjæringstidspunkt eq skjæringstidspunkt) and AvviksvurderingTable.slettet.isNull()
            }
                .orderBy(AvviksvurderingTable.opprettet to SortOrder.DESC)
                .limit(1)
                .singleOrNull { it.kilde != "INFOTRYGD" }
                ?.toDomain()
        }
    }

    internal fun insertOne(avviksvurderingsgrunnlag: Avviksvurderingsgrunnlag) {
        return transaction {
            AvviksvurderingRow.findById(avviksvurderingsgrunnlag.id) ?: insertAvviksvurdering(
                avviksvurderingsgrunnlag.id,
                avviksvurderingsgrunnlag.fødselsnummer,
                avviksvurderingsgrunnlag.skjæringstidspunkt,
                avviksvurderingsgrunnlag.opprettet,
                avviksvurderingsgrunnlag.sammenligningsgrunnlag,
                avviksvurderingsgrunnlag.beregningsgrunnlag
            )
        }
    }

    private fun Transaction.insertAvviksvurdering(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        beregningsgrunnlag: Beregningsgrunnlag,
    ){
        this.run {
            val avviksvurderingRow = AvviksvurderingRow.new(id) {
                this.fødselsnummer = fødselsnummer.value
                this.skjæringstidspunkt = skjæringstidspunkt
                this.opprettet = opprettet
                this.kilde = "SPINNVILL"
            }

            sammenligningsgrunnlag.inntekter.forEach { (arbeidsgiverreferanse, inntekter) ->
                val sammenligningsgrunnlagRow = SammenligningsgrunnlagRow.new {
                    this.avviksvurdering = avviksvurderingRow
                    this.arbeidsgiverreferanse = arbeidsgiverreferanse.value
                }

                MånedsinntektTable.batchInsert(inntekter) {
                    this[MånedsinntektTable.sammenligningsgrunnlag] = sammenligningsgrunnlagRow.id
                    this[MånedsinntektTable.inntekt] = it.inntekt.value
                    this[MånedsinntektTable.måned] = it.måned.monthValue
                    this[MånedsinntektTable.år] = it.måned.year
                    this[MånedsinntektTable.fordel] = it.fordel?.value
                    this[MånedsinntektTable.beskrivelse] = it.beskrivelse?.value
                    this[MånedsinntektTable.inntektstype] = it.inntektstype.tilDatabase()
                }
            }

            beregningsgrunnlag.omregnedeÅrsinntekter.batchInsert(avviksvurderingRow)
        }
    }

    private fun Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>.batchInsert(avviksvurderingRow: AvviksvurderingRow) {
        BeregningsgrunnlagTable.batchInsert(this.toList(), ignore = true) { (organisasjonsnummer, inntekt) ->
            this[BeregningsgrunnlagTable.organisasjonsnummer] = organisasjonsnummer.value
            this[BeregningsgrunnlagTable.inntekt] = inntekt.value
            this[BeregningsgrunnlagTable.avviksvurdering] = avviksvurderingRow.id
        }
    }

    private fun AvviksvurderingRow.toDomain(): Avviksvurderingsgrunnlag {
        return Avviksvurderingsgrunnlag(
            id = this.id.value,
            fødselsnummer = Fødselsnummer(this.fødselsnummer),
            skjæringstidspunkt = this.skjæringstidspunkt,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(
                inntekter = this.sammenligningsgrunnlag.map {
                    ArbeidsgiverInntekt(
                        arbeidsgiverreferanse = Arbeidsgiverreferanse(it.arbeidsgiverreferanse),
                        inntekter = it.inntekter.map { enMånedsinntekt ->
                            ArbeidsgiverInntekt.MånedligInntekt(
                                inntekt = InntektPerMåned(enMånedsinntekt.inntekt),
                                måned = enMånedsinntekt.yearMonth,
                                fordel = enMånedsinntekt.fordel?.let { Fordel(it) },
                                beskrivelse = enMånedsinntekt.beskrivelse?.let { Beskrivelse(it) },
                                inntektstype = enMånedsinntekt.inntektstype.tilInntektstype()
                            )
                        }
                    )
                }
            ),
            opprettet = opprettet,
            beregningsgrunnlag = Beregningsgrunnlag(
                omregnedeÅrsinntekter = this.beregningsgrunnlag
                    .associate { beregningsgrunnlag ->
                        Arbeidsgiverreferanse(beregningsgrunnlag.organisasjonsnummer) to OmregnetÅrsinntekt(beregningsgrunnlag.inntekt)
                    }
            )
        )
    }

    private fun String.tilInntektstype(): ArbeidsgiverInntekt.Inntektstype {
        return when (this) {
            "LØNNSINNTEKT" -> ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
            "NÆRINGSINNTEKT" -> ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT
            "PENSJON_ELLER_TRYGD" -> ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD
            "YTELSE_FRA_OFFENTLIGE" -> ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE
            else -> error("Kunne ikke mappe til Inntektstype, $this er ikke en gyldig Inntektstype")
        }
    }

    private fun ArbeidsgiverInntekt.Inntektstype.tilDatabase(): String {
        return when (this) {
            ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT -> "LØNNSINNTEKT"
            ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }
}
