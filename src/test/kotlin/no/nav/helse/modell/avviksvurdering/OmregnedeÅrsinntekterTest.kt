package no.nav.helse.modell.avviksvurdering

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OmregnedeÅrsinntekterTest {

    @Test
    fun `referential equals`() {
        val omregnedeÅrsinntekter = omregnedeÅrsinntekter("a1" to 400000.0)
        assertEquals(omregnedeÅrsinntekter, omregnedeÅrsinntekter)
    }

    @Test
    fun `structural equals`() {
        val omregnedeÅrsinntekter = omregnedeÅrsinntekter("a1" to 400000.0)
        assertEquals(omregnedeÅrsinntekter("a1" to 400000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `not equals - forskjellig arbeidsgiver`() {
        val omregnedeÅrsinntekter = omregnedeÅrsinntekter("a1" to 400000.0)
        assertNotEquals(omregnedeÅrsinntekter("a2" to 400000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `not equals - forskjellig beløp`() {
        val omregnedeÅrsinntekter = omregnedeÅrsinntekter("a1" to 400000.0)
        assertNotEquals(omregnedeÅrsinntekter("a1" to 500000.0), omregnedeÅrsinntekter)
    }

    @Test
    fun `not equals - ulikt antall arbeidsgivere`() {
        val omregnedeÅrsinntekter = omregnedeÅrsinntekter("a1" to 400000.0)
        assertNotEquals(omregnedeÅrsinntekter("a1" to 500000.0, "a2" to 500000.0), omregnedeÅrsinntekter)
    }

    private fun omregnedeÅrsinntekter(vararg arbeidsgivere: Pair<String, Double>) = OmregnedeÅrsinntekter(arbeidsgivere.toMap())
}