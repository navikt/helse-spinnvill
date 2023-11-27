package no.nav.helse.avviksvurdering

import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Arbeidsgiverreferanse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BeregningsgrunnlagTest {

    @Test
    fun `referential equals`() {
        val omregnedeÅrsinntekter = beregningsgrunnlag("a1" to 400000.0)
        assertEquals(omregnedeÅrsinntekter, omregnedeÅrsinntekter)
    }

    @Test
    fun `structural equals`() {
        val omregnedeÅrsinntekter = beregningsgrunnlag("a1" to 400000.0)
        assertEquals(beregningsgrunnlag("a1" to 400000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `not equals - forskjellig arbeidsgiver`() {
        val omregnedeÅrsinntekter = beregningsgrunnlag("a1" to 400000.0)
        assertNotEquals(beregningsgrunnlag("a2" to 400000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `not equals - forskjellig beløp`() {
        val omregnedeÅrsinntekter = beregningsgrunnlag("a1" to 400000.0)
        assertNotEquals(beregningsgrunnlag("a1" to 500000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `not equals - ulikt antall arbeidsgivere`() {
        val omregnedeÅrsinntekter = beregningsgrunnlag("a1" to 400000.0)
        assertNotEquals(beregningsgrunnlag("a1" to 500000.0, "a2" to 500000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `kan bare opprette gyldige beregningsgrunnlag`() {
        val omregnedeÅrsinntekter = emptyMap<Arbeidsgiverreferanse, OmregnetÅrsinntekt>()
        assertThrows<IllegalArgumentException> { Beregningsgrunnlag.opprett(omregnedeÅrsinntekter) }
    }

    private fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) = Beregningsgrunnlag.opprett(
        arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) }
    )
}