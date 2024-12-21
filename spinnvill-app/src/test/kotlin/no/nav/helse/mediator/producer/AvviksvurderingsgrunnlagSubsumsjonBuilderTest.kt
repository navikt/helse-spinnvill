package no.nav.helse.mediator.producer

import no.nav.helse.InntektPerMåned
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.helpers.*
import no.nav.helse.somArbeidsgiverref
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

class AvviksvurderingsgrunnlagSubsumsjonBuilderTest {

    @Test
    fun `bygg subsumsjonsmelding for avviksvurdering`() {
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 50000.0)

        val builder = AvviksvurderingSubsumsjonBuilder(
            id = UUID.randomUUID(),
            harAkseptabeltAvvik = false,
            avviksprosent = 26.0,
            maksimaltTillattAvvik = 25.0,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )

        val subsumsjonsmelding = builder.`8-30 ledd 2 punktum 1`()
        assertEquals(SubsumsjonProducer.Utfall.VILKAR_BEREGNET, subsumsjonsmelding.utfall)
        assertEquals("8-30", subsumsjonsmelding.paragraf)
        assertEquals(2, subsumsjonsmelding.ledd)
        assertEquals(1, subsumsjonsmelding.punktum)
        assertEquals(null, subsumsjonsmelding.bokstav)

        assertEquals(
            mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to mapOf(
                    "totalbeløp" to 600000.0,
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "arbeidsgiverreferanse" to "a1",
                            "inntekt" to 600000.0
                        )
                    )
                ),
                "sammenligningsgrunnlag" to mapOf(
                    "totalbeløp" to 600000.0,
                    "innrapporterteMånedsinntekter" to (1..12).map {
                        mapOf(
                            "måned" to YearMonth.of(2018, it),
                            "inntekter" to listOf(
                                mapOf(
                                    "arbeidsgiverreferanse" to "a1",
                                    "inntekt" to 50000.0,
                                    "fordel" to "En fordel",
                                    "beskrivelse" to "En beskrivelse",
                                    "inntektstype" to "LØNNSINNTEKT",
                                )
                            )
                        )
                    }
                )
            ),
            subsumsjonsmelding.input
        )
    }

    @Test
    fun `bygg subsumsjonsmelding for avviksvurdering for flere arbeidsgivere`() {
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 420000.0, "a2" to 180000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 35000.0, "a2" to 15000.0)

        val builder = AvviksvurderingSubsumsjonBuilder(
            id = UUID.randomUUID(),
            harAkseptabeltAvvik = false,
            avviksprosent = 26.0,
            maksimaltTillattAvvik = 25.0,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )

        val subsumsjonsmelding = builder.`8-30 ledd 2 punktum 1`()
        assertEquals(SubsumsjonProducer.Utfall.VILKAR_BEREGNET, subsumsjonsmelding.utfall)
        assertEquals("8-30", subsumsjonsmelding.paragraf)
        assertEquals(2, subsumsjonsmelding.ledd)
        assertEquals(1, subsumsjonsmelding.punktum)
        assertEquals(null, subsumsjonsmelding.bokstav)

        assertEquals(
            mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to mapOf(
                    "totalbeløp" to 600000.0,
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "arbeidsgiverreferanse" to "a1",
                            "inntekt" to 420000.0
                        ),
                        mapOf(
                            "arbeidsgiverreferanse" to "a2",
                            "inntekt" to 180000.0
                        )
                    )
                ),
                "sammenligningsgrunnlag" to mapOf(
                    "totalbeløp" to 600000.0,
                    "innrapporterteMånedsinntekter" to (1..12).map {
                        mapOf(
                            "måned" to YearMonth.of(2018, it),
                            "inntekter" to listOf(
                                mapOf(
                                    "arbeidsgiverreferanse" to "a1",
                                    "inntekt" to 35000.0,
                                    "fordel" to "En fordel",
                                    "beskrivelse" to "En beskrivelse",
                                    "inntektstype" to "LØNNSINNTEKT",
                                ),
                                mapOf(
                                    "arbeidsgiverreferanse" to "a2",
                                    "inntekt" to 15000.0,
                                    "fordel" to "En fordel",
                                    "beskrivelse" to "En beskrivelse",
                                    "inntektstype" to "LØNNSINNTEKT",
                                )
                            )
                        )
                    }
                )
            ),
            subsumsjonsmelding.input
        )
    }

    @Test
    fun `bygg avviksvurderingmelding med en arbeidsgiver og en inntekt per måned`() {
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 50000.0)

        val builder = AvviksvurderingSubsumsjonBuilder(
            id = UUID.randomUUID(),
            harAkseptabeltAvvik = false,
            avviksprosent = 26.0,
            maksimaltTillattAvvik = 25.0,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )

        val avviksvurdering = builder.buildAvvikVurdert()

        assertEquals(26.0, avviksvurdering.avviksprosent)
        assertEquals(600000.0, avviksvurdering.beregningsgrunnlagTotalbeløp)
        assertEquals(600000.0, avviksvurdering.sammenligningsgrunnlagTotalbeløp)
        assertEquals(1, avviksvurdering.omregnedeÅrsinntekter.size)
        assertEquals("a1".somArbeidsgiverref(), avviksvurdering.omregnedeÅrsinntekter.entries.single().key)
        assertEquals(OmregnetÅrsinntekt(600000.0), avviksvurdering.omregnedeÅrsinntekter.entries.single().value)
        assertEquals(1, avviksvurdering.innrapporterteInntekter.size)
        assertEquals("a1".somArbeidsgiverref(), avviksvurdering.innrapporterteInntekter.first().arbeidsgiverreferanse)
        assertEquals(12, avviksvurdering.innrapporterteInntekter.first().inntekter.size)
        assertEquals(InntektPerMåned(50000.0), avviksvurdering.innrapporterteInntekter.first().inntekter.first().beløp)
        assertEquals(YearMonth.from(1.januar), avviksvurdering.innrapporterteInntekter.first().inntekter.first().måned)
    }

    @Test
    fun `bygg avviksvurderingmelding med fler arbeidsgivere og inntekter per måned`() {
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0, "a2" to 240000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(
            listOf(
                YearMonth.from(1.januar),
                YearMonth.from(1.januar),
                YearMonth.from(1.februar),
                YearMonth.from(1.mars)
            ), "a1" to 50000.0, "a2" to 20000.0
        )

        val builder = AvviksvurderingSubsumsjonBuilder(
            id = UUID.randomUUID(),
            harAkseptabeltAvvik = false,
            avviksprosent = 26.0,
            maksimaltTillattAvvik = 25.0,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )

        val avviksvurdering = builder.buildAvvikVurdert()

        assertEquals(26.0, avviksvurdering.avviksprosent)
        assertEquals(840000.0, avviksvurdering.beregningsgrunnlagTotalbeløp)
        assertEquals(280000.0, avviksvurdering.sammenligningsgrunnlagTotalbeløp)
        assertEquals(2, avviksvurdering.omregnedeÅrsinntekter.size)
        assertEquals(OmregnetÅrsinntekt(600000.0), avviksvurdering.omregnedeÅrsinntekter["a1".somArbeidsgiverref()])
        assertEquals(OmregnetÅrsinntekt(240000.0), avviksvurdering.omregnedeÅrsinntekter["a2".somArbeidsgiverref()])
        assertEquals(2, avviksvurdering.innrapporterteInntekter.size)

        assertEquals("a1".somArbeidsgiverref(), avviksvurdering.innrapporterteInntekter[0].arbeidsgiverreferanse)
        assertEquals(4, avviksvurdering.innrapporterteInntekter[0].inntekter.size)
        val inntekterA1 = avviksvurdering.innrapporterteInntekter[0].inntekter
        assertEquals(InntektPerMåned(50000.0), inntekterA1[0].beløp)
        assertEquals(YearMonth.from(1.januar), inntekterA1[0].måned)
        assertEquals(InntektPerMåned(50000.0), inntekterA1[1].beløp)
        assertEquals(YearMonth.from(1.januar), inntekterA1[1].måned)
        assertEquals(InntektPerMåned(50000.0), inntekterA1[2].beløp)
        assertEquals(YearMonth.from(1.februar), inntekterA1[2].måned)
        assertEquals(InntektPerMåned(50000.0), inntekterA1[3].beløp)
        assertEquals(YearMonth.from(1.mars), inntekterA1[3].måned)

        assertEquals("a2".somArbeidsgiverref(), avviksvurdering.innrapporterteInntekter[1].arbeidsgiverreferanse)
        assertEquals(4, avviksvurdering.innrapporterteInntekter[1].inntekter.size)
        val inntekterA2 = avviksvurdering.innrapporterteInntekter[1].inntekter
        assertEquals(InntektPerMåned(20000.0), inntekterA2[0].beløp)
        assertEquals(YearMonth.from(1.januar), inntekterA2[0].måned)
        assertEquals(InntektPerMåned(20000.0), inntekterA2[1].beløp)
        assertEquals(YearMonth.from(1.januar), inntekterA2[1].måned)
        assertEquals(InntektPerMåned(20000.0), inntekterA2[2].beløp)
        assertEquals(YearMonth.from(1.februar), inntekterA2[2].måned)
        assertEquals(InntektPerMåned(20000.0), inntekterA2[3].beløp)
        assertEquals(YearMonth.from(1.mars), inntekterA2[3].måned)
    }
}
