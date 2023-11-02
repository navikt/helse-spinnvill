package no.nav.helse.modell.avviksvurdering

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksprosentTest {

    @Test
    fun `har ikke akseptabelt avvik`() {
        val avviksprosent = Avviksprosent.avvik(400000.0, 600000.0)
        assertFalse(avviksprosent.harAkseptabeltAvvik())
    }
    @Test
    fun `har akseptabelt avvik`() {
        val avviksprosent = Avviksprosent.avvik(500000.0, 600000.0)
        assertTrue(avviksprosent.harAkseptabeltAvvik())
    }
    @Test
    fun `har akseptabelt avvik når avvik er på grensen`() {
        val avviksprosent = Avviksprosent.avvik(300000.0, 400000.0)
        assertTrue(avviksprosent.harAkseptabeltAvvik())
    }
    @Test
    fun `kan ha avviksprosent høyere enn 100 prosent`() {
        val avviksprosent = Avviksprosent.avvik(600000.0, 200000.0)
        assertFalse(avviksprosent.harAkseptabeltAvvik())
    }

    @Test
    fun equals() {
        val avviksprosent1 = Avviksprosent.avvik(100000.0, 100000.0)
        val avviksprosent2 = Avviksprosent.avvik(100000.0, 100000.0)
        assertEquals(avviksprosent1, avviksprosent2)
    }
    @Test
    fun `når diff er mindre enn epsilon er prosentene like`() {
        val avviksprosent1 = Avviksprosent.avvik(1.000002, 1.000003)
        val avviksprosent2 = Avviksprosent.avvik(1.0, 1.0)
        assertEquals(avviksprosent1, avviksprosent2)
    }
    @Test
    fun `når diff er større enn epsilon er prosentene ikke like`() {
        val avviksprosent1 = Avviksprosent.avvik(1.00001, 1.00002)
        val avviksprosent2 = Avviksprosent.avvik(1.0, 1.0)
        assertNotEquals(avviksprosent1, avviksprosent2)
    }
}