package no.nav.helse.avviksvurdering

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksprosentTest {

    @Test
    fun `avrunding til fire desimaler`() {
        val avviksprosent = Avviksprosent.avvik(12.50001, 10.0)
        assertEquals(25.0001, avviksprosent.avrundetTilFireDesimaler)
    }

    @Test
    fun INGEN() {
        assertEquals(-1.0, Avviksprosent.INGEN.avrundetTilFireDesimaler)
    }

    @Test
    fun `når diff i avviksprosent blir mindre enn 0,00005 er avvikene like`() {
        val avviksprosent1 = Avviksprosent.avvik(600000.24, 600000.0)
        val intetAvvik = Avviksprosent.avvik(1.0, 1.0)
        assertEquals(avviksprosent1, intetAvvik)
        assertFalse(avviksprosent1 < intetAvvik)
        assertFalse(avviksprosent1 > intetAvvik)
    }

    @Test
    fun `når diff i avviksprosent blir lik 0,00005 er avvikene ulike`() {
        val avviksprosent1 = Avviksprosent.avvik(600000.3, 600000.0)
        val intetAvvik = Avviksprosent.avvik(1.0, 1.0)
        assertNotEquals(avviksprosent1, intetAvvik)
        assertTrue(avviksprosent1 > intetAvvik)
    }

    @Test
    fun `når diff i avviksprosent blir lik 0,00005 er avvikene ulike2`() {
        val avviksprosent1 = Avviksprosent.avvik(600000.3, 600000.0)
        assertEquals(Avviksprosent(0.00005), avviksprosent1)
    }

    @Test
    fun compareTo() {
        assertEquals(0, Avviksprosent(10.00049).compareTo(Avviksprosent(10.0005)))
        assertEquals(1, Avviksprosent(10.0006).compareTo(Avviksprosent(10.0005)))
        assertEquals(-1, Avviksprosent(10.0005).compareTo(Avviksprosent(10.0006)))
    }

    @Test
    fun equals() {
        assertEquals(Avviksprosent(10.00049), Avviksprosent(10.0005))
        assertNotEquals(Avviksprosent(10.0006), (Avviksprosent(10.0005)))
        assertNotEquals(Avviksprosent(10.0005), (Avviksprosent(10.0006)))
    }
}