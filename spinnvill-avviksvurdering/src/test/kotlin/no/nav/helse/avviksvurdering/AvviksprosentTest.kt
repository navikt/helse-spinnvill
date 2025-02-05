package no.nav.helse.avviksvurdering

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksprosentTest {

    private val nullAvvik = Avviksprosent(0.0)

    @Test
    fun INGEN() {
        assertEquals(-1.0, Avviksprosent.INGEN.avrundetTilFireDesimaler)
    }

    @Test
    fun `beregning og avrunding gir korrekt avviksprosent`() {
        val avviksprosent = Avviksprosent.avvik(12.50001, 10.0)
        assertEquals(25.0001, avviksprosent.avrundetTilFireDesimaler)
    }

    @Test
    fun `avviksprosent under grenseverdi gir ikke avvik`() {
        val avviksprosentUnderGrenseverdi = Avviksprosent(0.000049)
        assertEquals(avviksprosentUnderGrenseverdi, nullAvvik)
        assertFalse(avviksprosentUnderGrenseverdi < nullAvvik)
        assertFalse(avviksprosentUnderGrenseverdi > nullAvvik)
    }

    @Test
    fun `avviksprosent over grenseverdi gir avvik`() {
        assertEquals(Avviksprosent(0.0001), Avviksprosent(0.00005))
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