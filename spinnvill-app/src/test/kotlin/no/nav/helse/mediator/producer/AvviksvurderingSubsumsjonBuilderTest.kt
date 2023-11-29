package no.nav.helse.mediator.producer

import no.nav.helse.helpers.beregningsgrunnlag
import no.nav.helse.helpers.sammenligningsgrunnlag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

class AvviksvurderingSubsumsjonBuilderTest {

    @Test
    fun `bygg subsumsjonsmelding for avviksvurdering`() {
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 50000.0)

        val builder = AvviksvurderingSubsumsjonBuilder(
            false, 26.0, 25.0, beregningsgrunnlag, sammenligningsgrunnlag
        )

        val subsumsjonsmelding = builder.build()
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
            false, 26.0, 25.0, beregningsgrunnlag, sammenligningsgrunnlag
        )

        val subsumsjonsmelding = builder.build()
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
}