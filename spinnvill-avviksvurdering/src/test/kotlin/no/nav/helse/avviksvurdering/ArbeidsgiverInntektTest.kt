package no.nav.helse.avviksvurdering

import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Companion.sum
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class ArbeidsgiverInntektTest {

    private fun Double.rundTilToDesimaler() = (this * 100).roundToInt() / 100.0

    @Test
    fun sum() {
        val arbeidsgiverInntekter = listOf(
            ArbeidsgiverInntekt("a1", List(12) { 10000.0 }),
            ArbeidsgiverInntekt("a2", List(12) { 20000.0 })
        )
        assertEquals(360000.0, arbeidsgiverInntekter.sum().rundTilToDesimaler())
    }

    @Test
    fun `sum med desimaltall`() {
        val arbeidsgiverInntekter = listOf(
            ArbeidsgiverInntekt("a1", List(12) { 10000.10 }),
            ArbeidsgiverInntekt("a2", List(12) { 20000.20 })
        )
        assertEquals(360003.60, arbeidsgiverInntekter.sum().rundTilToDesimaler())
    }
}