package no.nav.helse.modell.avviksvurdering

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksvurderingTest{

    @Test
    fun`har gjort avviksvurdering før med en arbeidsgiver og samme inntekt` () {
        val avviksvurdering = Avviksvurdering(omregnedeÅrsinntekter("a1" to 600000.0), sammenligningsgrunnlag(50000.0))
        assertTrue(avviksvurdering.avviksvurderingGjortFor(omregnedeÅrsinntekter("a1" to 600000.0)))
    }

    @Test
    fun`har gjort avviksvurdering før med flere arbeidsgivere og samme inntekter` () {
        val avviksvurdering = Avviksvurdering(omregnedeÅrsinntekter("a1" to 600000.0, "a2" to 600000.0), sammenligningsgrunnlag(50000.0))
        assertTrue(avviksvurdering.avviksvurderingGjortFor(omregnedeÅrsinntekter("a1" to 600000.0, "a2" to 600000.0)))
    }

    @Test
    fun`har gjort avviksvurdering før med flere arbeidsgivere og samme inntekter - rekkefølge har ikke betydning` () {
        val avviksvurdering = Avviksvurdering(omregnedeÅrsinntekter("a2" to 200000.0, "a1" to 500000.0), sammenligningsgrunnlag(50000.0))
        assertTrue(avviksvurdering.avviksvurderingGjortFor(omregnedeÅrsinntekter("a1" to 500000.0, "a2" to 200000.0)))
    }

    @Test
    fun`har ikke gjort avviksvurdering før med en arbeidsgiver og forskjellig inntekt` () {
        val avviksvurdering = Avviksvurdering(omregnedeÅrsinntekter("a1" to 300000.0), sammenligningsgrunnlag(50000.0))
        assertFalse(avviksvurdering.avviksvurderingGjortFor(omregnedeÅrsinntekter("a1" to 600000.0)))
    }

    @Test
    fun`har ikke gjort avviksvurdering før med annen arbeidsgiver men samme inntekt` () {
        val avviksvurdering = Avviksvurdering(omregnedeÅrsinntekter("a1" to 600000.0), sammenligningsgrunnlag(50000.0))
        assertFalse(avviksvurdering.avviksvurderingGjortFor(omregnedeÅrsinntekter("a2" to 300000.0)))
    }

    @Test
    fun`har ikke gjort avviksvurdering før med ulikt antall arbeidsgivere` () {
        val avviksvurdering = Avviksvurdering(omregnedeÅrsinntekter("a1" to 600000.0), sammenligningsgrunnlag(50000.0))
        assertFalse(avviksvurdering.avviksvurderingGjortFor(omregnedeÅrsinntekter("a1" to 300000.0, "a2" to 300000.0)))
    }


    private fun sammenligningsgrunnlag(inntekt: Double) = Sammenligningsgrunnlag(List(12) { inntekt })

    private fun omregnedeÅrsinntekter(vararg arbeidsgivere: Pair<String, Double>) = OmregnedeÅrsinntekter(arbeidsgivere.toMap())
}