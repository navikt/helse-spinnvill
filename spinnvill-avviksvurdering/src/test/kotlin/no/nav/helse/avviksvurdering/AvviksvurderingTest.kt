package no.nav.helse.avviksvurdering

import no.nav.helse.*
import no.nav.helse.avviksvurdering.Avviksvurdering.Companion.siste
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.test.assertIs

internal class AvviksvurderingTest {

    @Test
    fun `har gjort avviksvurdering før`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.0))
        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.0))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat)
    }

    @Test
    fun `har fire desimalers oppløsning på avviksprosent`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a" to 750000.6))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
        assertEquals(25.0001, resultat.vurdering.avviksprosent)
    }

    @Test
    fun `gjør ny avviksvurdering når vi sammenligner beregningsgrunnlag med beløo 0 mot INGEN`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 0.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi allerede har avviksvurdert og beregningsgrunnlag bare er litt forskjellig`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.0))
        val resultat1 = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.1))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat1)

        val resultat2 = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 599999.88888884))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat2)
    }

    @Test
    fun `gjør ny avviksvurdering når vi allerede har avviksvurdert og beregningsgrunnlag er akkurat bare litt forskjellig`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 599999.88888884))

        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.99999994))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ny avviksvurdering når vi allerede har avviksvurdert og beregningsgrunnlag er rett over bare litt forskjellig`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.0))

        val resultat1 = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.9999999994))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat1)

        val resultat2 = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600001.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat2)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik innenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 600000.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
        val (_, harAkseptabeltAvvik, avviksprosent) = resultat.vurdering
        assertEquals(true, harAkseptabeltAvvik)
        assertEquals(0.0, avviksprosent)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik akkurat utenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 449999.4))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
        val (_, harAkseptabeltAvvik, avviksprosent) = resultat.vurdering
        assertEquals(25.0001, avviksprosent)
        assertFalse(harAkseptabeltAvvik) {"Forventet at $avviksprosent er et uakseptabelt avvik"}
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik akkurat innenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))

        val resultat = avviksvurdering.vurderAvvik(beregningsgrunnlag("a1" to 449999.7))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
        val (_, harAkseptabeltAvvik, avviksprosent) = resultat.vurdering
        assertEquals(25.0, avviksprosent)
        assertTrue(harAkseptabeltAvvik) {"Forventet at $avviksprosent er et akseptabelt avvik"}
    }

    @Test
    fun `finn siste avviksvurdering fra liste`() {
        val avviksvurdering1 = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        val avviksvurdering2 = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        val avviksvurderinger = listOf(avviksvurdering1, avviksvurdering2)
        assertEquals(avviksvurdering2, avviksvurderinger.siste())
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
        Beregningsgrunnlag.opprett(arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) })
}

