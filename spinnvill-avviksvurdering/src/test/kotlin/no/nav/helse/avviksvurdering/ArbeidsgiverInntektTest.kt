package no.nav.helse.avviksvurdering

import no.nav.helse.InntektPerMåned
import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Companion.sum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.math.roundToInt

class ArbeidsgiverInntektTest {

    private fun Double.rundTilToDesimaler() = (this * 100).roundToInt() / 100.0

    @Test
    fun sum() {
        val arbeidsgiverInntekter = listOf(
            ArbeidsgiverInntekt(Arbeidsgiverreferanse("a1"), inntekter(10000.0)),
            ArbeidsgiverInntekt(Arbeidsgiverreferanse("a2"), inntekter(20000.0))
        )
        assertEquals(360000.0, arbeidsgiverInntekter.sum().rundTilToDesimaler())
    }

    @Test
    fun `sum med desimaltall`() {
        val arbeidsgiverInntekter = listOf(
            ArbeidsgiverInntekt(Arbeidsgiverreferanse("a1"), inntekter(10000.10)),
            ArbeidsgiverInntekt(Arbeidsgiverreferanse("a2"), inntekter(20000.20))
        )
        assertEquals(360003.60, arbeidsgiverInntekter.sum().rundTilToDesimaler())
    }

    private fun inntekter(beløp: Double) = List(12) { YearMonth.of(2018, it + 1) to InntektPerMåned(beløp) }.toMap()
}
