package no.nav.helse.mediator.producer

import no.nav.helse.*
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
    private val sammenligningsgrunnlagDto = SammenligningsgrunnlagBuilder().buildForSubsumsjon(sammenligningsgrunnlag)

    internal fun buildAvviksvurdering(): AvviksvurderingProducer.AvviksvurderingDto {
        return AvviksvurderingProducer.AvviksvurderingDto(
            avviksprosent = avviksprosent,
            beregningsgrunnlagTotalbeløp = beregningsgrunnlagDto.totalbeløp,
            omregnedeÅrsinntekter = beregningsgrunnlagDto.omregnedeÅrsinntekter,
            sammenligningsgrunnlagTotalbeløp = sammenligningsgrunnlagDto.totalbeløp,
            innrapporterteInntekter = sammenligningsgrunnlagDto.månedligeInntekter.tilInnrapporterteInntekter()
        )
    }

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
                                    "arbeidsgiverreferanse" to it.arbeidsgiverreferanse.value,
                                    "inntekt" to it.inntekt.value,
                                    "fordel" to it.fordel?.value,
                                    "beskrivelse" to it.beskrivelse?.value,
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

    private fun Map<YearMonth, List<MånedligInntekt>>.tilInnrapporterteInntekter(): List<AvviksvurderingProducer.AvviksvurderingDto.InnrapportertInntekt> =
        entries.flatMap { (yearMonth, månedligInntektList) ->
            månedligInntektList.map {
                AvviksvurderingProducer.AvviksvurderingDto.InnrapportertInntekt(
                    arbeidsgiverreferanse = it.arbeidsgiverreferanse,
                    inntekter = mapOf(yearMonth to it.inntekt)
                )
            }
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
        val arbeidsgiverreferanse: Arbeidsgiverreferanse,
        val inntekt: InntektPerMåned,
        val fordel: Fordel?,
        val beskrivelse: Beskrivelse?,
        val inntektstype: String
    )

    private data class ArbeidsgiverligInntekt(
        val måned: YearMonth,
        val inntekt: InntektPerMåned,
        val fordel: Fordel?,
        val beskrivelse: Beskrivelse?,
        val inntektstype: String
    )

    private class BeregningsgrunnlagBuilder : Visitor {
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

    private class SammenligningsgrunnlagBuilder : Visitor {
        private var totalbeløp by Delegates.notNull<Double>()
        private val arbeidsgiverInntekter = mutableMapOf<Arbeidsgiverreferanse, MutableList<ArbeidsgiverligInntekt>>()

        override fun visitSammenligningsgrunnlag(sammenligningsgrunnlag: Double) {
            totalbeløp = sammenligningsgrunnlag
        }

        override fun visitArbeidsgiverInntekt(
            arbeidsgiverreferanse: Arbeidsgiverreferanse,
            inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>
        ) {
            arbeidsgiverInntekter.getOrPut(arbeidsgiverreferanse) { mutableListOf() }.addAll(inntekter.map {
                ArbeidsgiverligInntekt(
                    måned = it.måned,
                    inntekt = it.inntekt,
                    fordel = it.fordel,
                    beskrivelse = it.beskrivelse,
                    inntektstype = it.inntektstype.toSubsumsjonString()
                )
            })
        }

        fun buildForSubsumsjon(sammenligningsgrunnlag: Sammenligningsgrunnlag): SammenligningsgrunnlagDto {
            sammenligningsgrunnlag.accept(this)

            return SammenligningsgrunnlagDto(
                totalbeløp = totalbeløp,
                månedligeInntekter = arbeidsgiverInntekter
                    .flatMap { (arbeidsgiverreferanse, inntekter) ->
                        inntekter.map { inntekt ->
                            inntekt.måned to MånedligInntekt(
                                arbeidsgiverreferanse = arbeidsgiverreferanse,
                                inntekt = inntekt.inntekt,
                                fordel = inntekt.fordel,
                                beskrivelse = inntekt.beskrivelse,
                                inntektstype = inntekt.inntektstype
                            )
                        }
                    }
                    .groupBy( { it.first }) { it.second }
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