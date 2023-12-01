package no.nav.helse.mediator.producer

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.avviksvurdering.Visitor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.properties.Delegates

internal class AvviksvurderingSubsumsjonBuilder(
    private val harAkseptabeltAvvik: Boolean,
    private val avviksprosent: Double,
    private val maksimaltTillattAvvik: Double,
    beregningsgrunnlag: Beregningsgrunnlag,
    sammenligningsgrunnlag: Sammenligningsgrunnlag,
) {
    private val beregningsgrunnlagDto = BeregningsgrunnlagBuilder().build(beregningsgrunnlag)
    private val sammenligningsgrunnlagDto = SammenligningsgrunnlagBuilder().build(sammenligningsgrunnlag)

    internal fun buildSubsumsjon(): SubsumsjonProducer.SubsumsjonsmeldingDto {
        return SubsumsjonProducer.SubsumsjonsmeldingDto(
            paragraf = "8-30",
            ledd = 2,
            bokstav = null,
            punktum = 1,
            lovverk = "folketrygdloven",
            lovverksversjon = LocalDate.of(2019, 1, 1),
            utfall = SubsumsjonProducer.Utfall.VILKAR_BEREGNET,
            input = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to maksimaltTillattAvvik,
                "grunnlagForSykepengegrunnlag" to mapOf(
                    "totalbeløp" to beregningsgrunnlagDto.totalbeløp,
                    "omregnedeÅrsinntekter" to beregningsgrunnlagDto.omregnedeÅrsinntekter.map { (arbeidsgiverreferanse, omregnetÅrsinntekt) ->
                        mapOf(
                            "arbeidsgiverreferanse" to arbeidsgiverreferanse.value,
                            "inntekt" to omregnetÅrsinntekt.value
                        )
                    }
                ),
                "sammenligningsgrunnlag" to mapOf(
                    "totalbeløp" to sammenligningsgrunnlagDto.totalbeløp,
                    "innrapporterteMånedsinntekter" to sammenligningsgrunnlagDto.månedligeInntekter.map { (måned, inntekter) ->
                        mapOf(
                            "måned" to måned,
                            "inntekter" to inntekter.map {
                                mapOf(
                                    "arbeidsgiverreferanse" to it.arbeidsgiverreferanse,
                                    "inntekt" to it.inntekt,
                                    "fordel" to it.fordel,
                                    "beskrivelse" to it.beskrivelse,
                                    "inntektstype" to it.inntektstype,
                                )
                            }
                        )
                    }
                )
            ),
            output = mapOf(
                "avviksprosent" to avviksprosent,
                "harAkseptabeltAvvik" to harAkseptabeltAvvik
            )
        )
    }

    private data class BeregningsgrunnlagDto(
        val totalbeløp: Double,
        val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>
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

    private class BeregningsgrunnlagBuilder: Visitor {
        private lateinit var beregningsgrunnlagDto: BeregningsgrunnlagDto
        override fun visitBeregningsgrunnlag(
            totaltOmregnetÅrsinntekt: Double,
            omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>
        ) {
            beregningsgrunnlagDto = BeregningsgrunnlagDto(
                totalbeløp = totaltOmregnetÅrsinntekt,
                omregnedeÅrsinntekter = omregnedeÅrsinntekter
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