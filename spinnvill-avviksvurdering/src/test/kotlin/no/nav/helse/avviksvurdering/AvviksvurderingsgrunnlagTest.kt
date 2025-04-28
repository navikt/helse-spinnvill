package no.nav.helse.avviksvurdering

import no.nav.helse.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class AvviksvurderingsgrunnlagTest {

    @Test
    fun `har fire desimalers oppløsning på avviksprosent`() {
        val grunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0), beregningsgrunnlag("a1" to 750000.6))
        val vurdering = grunnlag.avviksvurdering()
        assertEquals(25.0001, vurdering.avviksprosent)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik innenfor akseptabelt avvik`() {
        val grunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0), beregningsgrunnlag("a1" to 600000.0))
        val vurdering = grunnlag.avviksvurdering()
        val (_, harAkseptabeltAvvik, avviksprosent) = vurdering
        assertEquals(true, harAkseptabeltAvvik)
        assertEquals(0.0, avviksprosent)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik akkurat utenfor akseptabelt avvik`() {
        val grunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0), beregningsgrunnlag("a1" to 449999.4))
        val vurdering = grunnlag.avviksvurdering()
        val (_, harAkseptabeltAvvik, avviksprosent) = vurdering
        assertEquals(25.0001, avviksprosent)
        assertFalse(harAkseptabeltAvvik) {"Forventet at $avviksprosent er et uakseptabelt avvik"}
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik akkurat innenfor akseptabelt avvik`() {
        val grunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0), beregningsgrunnlag("a1" to 449999.7))

        val vurdering = grunnlag.avviksvurdering()
        val (_, harAkseptabeltAvvik, avviksprosent) = vurdering
        assertEquals(25.0, avviksprosent)
        assertTrue(harAkseptabeltAvvik) {"Forventet at $avviksprosent er et akseptabelt avvik"}
    }

    // tilfelle fra produksjon (en biarbeidsgiver som trakk tilbake tidligere utbetalt bonus)
    // https://nav-it.slack.com/archives/C014X6VBFPV/p1743586076621489
    @Test
    fun `man får et positivt avvikstall også når sammenligningsgrunnlaget er negativt`() {
        val grunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(-103.1225), beregningsgrunnlag("a1" to 67_680.0))
        val vurdering = grunnlag.avviksvurdering()
        val avviksprosent = vurdering.avviksprosent
        assertTrue(avviksprosent > 0) { "Forventet at $avviksprosent er et positivt prosenttall" }
    }

    @Test
    fun `sammenlign beregningsgrunnlaget i avviksvurderingsgrunnlag med et beregningsgrunnlag når de er like`() {
        val avviksvurderingsgrunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0), beregningsgrunnlag("a1" to 600000.0))
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)

        assertTrue(avviksvurderingsgrunnlag.beregningsgrunnlagLiktSom(beregningsgrunnlag))
    }

    @Test
    fun `sammenlign beregningsgrunnlaget i avviksvurderingsgrunnlag med et beregningsgrunnlag når de er forskjellige`() {
        val avviksvurderingsgrunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0), beregningsgrunnlag("a1" to 600000.0))
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600001.0)

        assertFalse(avviksvurderingsgrunnlag.beregningsgrunnlagLiktSom(beregningsgrunnlag))
    }

    private fun sammenligningsgrunnlag(inntekt: Double) = Sammenligningsgrunnlag(
        listOf(
            ArbeidsgiverInntekt(Arbeidsgiverreferanse("a1"), List(12) {
                ArbeidsgiverInntekt.MånedligInntekt(
                    inntekt = InntektPerMåned(inntekt),
                    måned = YearMonth.of(2018, it + 1),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                )
            })
        )
    )

    private fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) =
        Beregningsgrunnlag(arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) })
}

