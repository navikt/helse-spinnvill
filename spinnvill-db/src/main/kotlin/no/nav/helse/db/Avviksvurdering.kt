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

    internal fun findLatest(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurderingsgrunnlag? {
        return transaction {
            EnAvviksvurdering.find {
                Avviksvurderinger.fødselsnummer eq fødselsnummer.value and (Avviksvurderinger.skjæringstidspunkt eq skjæringstidspunkt) and Avviksvurderinger.slettet.isNull()
            }
                .orderBy(Avviksvurderinger.opprettet to SortOrder.DESC)
                .limit(1)
                .singleOrNull { it.kilde != "INFOTRYGD" }
                ?.toDomain()
        }
    }

    internal fun insertOne(avviksvurderingsgrunnlag: Avviksvurderingsgrunnlag) {
        return transaction {
            EnAvviksvurdering.findById(avviksvurderingsgrunnlag.id) ?: insertAvviksvurdering(
                avviksvurderingsgrunnlag.id,
                avviksvurderingsgrunnlag.fødselsnummer,
                avviksvurderingsgrunnlag.skjæringstidspunkt,
                avviksvurderingsgrunnlag.kilde.name,
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
        kilde: String,
        opprettet: LocalDateTime,
        sammenligningsgrunnlag: no.nav.helse.avviksvurdering.Sammenligningsgrunnlag,
        beregningsgrunnlag: no.nav.helse.avviksvurdering.Beregningsgrunnlag,
    ){
        this.run {
            val enAvviksvurdering = EnAvviksvurdering.new(id) {
                this.fødselsnummer = fødselsnummer.value
                this.skjæringstidspunkt = skjæringstidspunkt
                this.opprettet = opprettet
                this.kilde = kilde
            }

            sammenligningsgrunnlag.inntekter.forEach { (arbeidsgiverreferanse, inntekter) ->
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

            beregningsgrunnlag.omregnedeÅrsinntekter.batchInsert(enAvviksvurdering)
        }
    }

    private fun Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>.batchInsert(enAvviksvurdering: EnAvviksvurdering) {
        Beregningsgrunnlag.batchInsert(this.toList(), ignore = true) { (organisasjonsnummer, inntekt) ->
            this[Beregningsgrunnlag.organisasjonsnummer] = organisasjonsnummer.value
            this[Beregningsgrunnlag.inntekt] = inntekt.value
            this[Beregningsgrunnlag.avviksvurdering] = enAvviksvurdering.id
        }
    }

    private fun EnAvviksvurdering.toDomain(): Avviksvurderingsgrunnlag {
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
            kilde = this.kilde.tilKilde(),
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

    private fun String.tilKilde(): Kilde {
        return when (this) {
            "SPINNVILL" -> Kilde.SPINNVILL
            "SPLEIS" -> Kilde.SPLEIS
            "INFOTRYGD" -> Kilde.INFOTRYGD
            else -> error("Kunne ikke mappe til Kilde, $this er ikke en gyldig Kilde")
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
