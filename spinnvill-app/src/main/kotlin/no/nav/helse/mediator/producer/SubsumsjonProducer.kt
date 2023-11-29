package no.nav.helse.mediator.producer

import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.avviksvurdering.Visitor
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.properties.Delegates

internal class SubsumsjonProducer(
    private val fødselsnummer: Fødselsnummer,
    private val versjonAvKode: VersjonAvKode,
    private val rapidsConnection: RapidsConnection
) : KriterieObserver {
    private val subsumsjonskø = mutableListOf<SubsumsjonsmeldingDto>()

    override fun avvikVurdert(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double,
    ) {
        val beregningsgrunnlagDto = BeregningsgrunnlagBuilder().build(beregningsgrunnlag)
        val sammenligningsgrunnlagDto = SammenligningsgrunnlagBuilder().build(sammenligningsgrunnlag)
        subsumsjonskø.add(
            SubsumsjonsmeldingDto(
                paragraf = "8-30",
                ledd = "2",
                bokstav = null,
                punktum = 1,
                lovverk = "folketrygdloven",
                lovverksversjon = LocalDate.of(2019, 1, 1),
                utfall = Utfall.VILKAR_BEREGNET,
                input = mapOf(
                    "maksimaltTillattAvvikPåÅrsinntekt" to maksimaltTillattAvvik,
                    "grunnlagForSykepengegrunnlag" to mapOf(
                        "totalbeløp" to beregningsgrunnlagDto.totalbeløp,
                        "omregnedeÅrsinntekter" to beregningsgrunnlagDto.omregnedeÅrsinntekter.map {
                            mapOf(
                                "arbeidsgiverreferanse" to it.key.value,
                                "inntekt" to it.value
                            )
                        }
                    ),
                    "sammenligningsgrunnlag" to mapOf(
                        "totalbeløp" to sammenligningsgrunnlagDto.totalbeløp,
                        "månedligeInntekter" to sammenligningsgrunnlagDto.månedligeInntekter.map { (måned, inntekter) ->
                            mapOf(
                                "måned" to måned,
                                "inntekter" to inntekter
                            )
                        }
                    )
                ),
                output = mapOf(
                    "avviksprosent" to avviksprosent,
                    "harAkseptabeltAvvik" to harAkseptabeltAvvik
                ),
                sporing = emptyMap()
            )
        )
    }

    internal fun finalize() {
        subsumsjonskø.forEach {
            rapidsConnection.publish(fødselsnummer.value, JsonMessage.newMessage(
                "subsumsjon",
                mapOf(
                    "subsumsjon" to mutableMapOf(
                        "fodselsnummer" to fødselsnummer.value,
                        "id" to it.id,
                        "tidsstempel" to it.tidsstempel,
                        "kilde" to "spinnvill",
                        "versjon" to "1.0.0",
                        "paragraf" to it.paragraf,
                        "lovverk" to it.lovverk,
                        "lovverksversjon" to it.lovverksversjon,
                        "utfall" to it.utfall,
                        "input" to it.input,
                        "output" to it.output,
                        "sporing" to it.sporing,
                        "versjonAvKode" to versjonAvKode
                    ).apply {
                        compute("ledd") { _, _ -> it.ledd }
                        compute("bokstav") { _, _ -> it.bokstav }
                        compute("punktum") { _, _ -> it.punktum }
                    },
                )
            ).toJson())
        }
        subsumsjonskø.clear()
    }

    private data class BeregningsgrunnlagDto(
        val totalbeløp: Double,
        val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, Double>
    )

    private data class SammenligningsgrunnlagDto(
        val totalbeløp: Double,
        val månedligeInntekter: Map<YearMonth, List<MånedligInntekt>>
    )

    private data class MånedligInntekt(
        val arbeidsgiverreferanse: String,
        val inntekt: Double,
        val fordel: String?,
        val beskrivelse: String?,
        val inntektstype: String
    )

    private data class SubsumsjonsmeldingDto(
        val paragraf: String,
        val ledd: String?,
        val bokstav: String?,
        val punktum: Int?,
        val lovverk: String,
        val lovverksversjon: LocalDate,
        val utfall: Utfall,
        val input: Map<String, Any>,
        val output: Map<String, Any>,
        val sporing: Map<String, List<String>>,
    ) {
        val id: UUID = UUID.randomUUID()
        val tidsstempel: LocalDateTime = LocalDateTime.now()
    }

    private enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }

    private class BeregningsgrunnlagBuilder: Visitor {
        private lateinit var beregningsgrunnlagDto: BeregningsgrunnlagDto
        override fun visitBeregningsgrunnlag(
            totaltOmregnetÅrsinntekt: Double,
            omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>
        ) {
            beregningsgrunnlagDto = BeregningsgrunnlagDto(
                totalbeløp = totaltOmregnetÅrsinntekt,
                omregnedeÅrsinntekter = omregnedeÅrsinntekter.mapValues { (_, inntekt) ->
                    inntekt.value
                }
            )
        }

        fun build(beregningsgrunnlag: Beregningsgrunnlag): BeregningsgrunnlagDto {
            beregningsgrunnlag.accept(this)
            return beregningsgrunnlagDto
        }
    }
    private class SammenligningsgrunnlagBuilder: Visitor {
        private var totalbeløp by Delegates.notNull<Double>()
        private val inntekter = mutableMapOf<YearMonth, MutableList<MånedligInntekt>>()

        override fun visitSammenligningsgrunnlag(sammenligningsgrunnlag: Double) {
            totalbeløp = sammenligningsgrunnlag
        }

        override fun visitArbeidsgiverInntekt(
            arbeidsgiverreferanse: Arbeidsgiverreferanse,
            inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>
        ) {
            val inntekterPerMåned = inntekter.groupBy {
                it.måned
            }.mapValues {(_, inntekter) ->
                inntekter.map {
                    MånedligInntekt(
                        arbeidsgiverreferanse.value,
                        it.inntekt.value,
                        it.fordel?.value,
                        it.beskrivelse?.value,
                        it.inntektstype.toSubsumsjonString()
                    )
                }
            }

            inntekterPerMåned.forEach { (måned, inntekter) ->
                this.inntekter.getOrPut(måned) { mutableListOf() }.addAll(inntekter)
            }
        }

        fun build(sammenligningsgrunnlag: Sammenligningsgrunnlag): SammenligningsgrunnlagDto {
            sammenligningsgrunnlag.accept(this)
            return SammenligningsgrunnlagDto(
                totalbeløp = totalbeløp,
                månedligeInntekter = inntekter
            )
        }

        fun ArbeidsgiverInntekt.Inntektstype.toSubsumsjonString() = when (this) {
            ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT -> "LØNNSINNTEKT"
            ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }
}