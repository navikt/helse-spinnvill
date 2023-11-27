package no.nav.helse.avviksvurdering

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.InntektPerMåned
import java.time.YearMonth

class ArbeidsgiverInntekt(
    private val arbeidsgiverreferanse: Arbeidsgiverreferanse,
    private val inntekter: Map<YearMonth, InntektPerMåned>
) {
    operator fun plus(other: Double) = other + inntekter.values.sumOf { it.value }

    internal companion object {
        internal fun Iterable<ArbeidsgiverInntekt>.sum(): Double {
            return this.fold(0.0) { acc, arbeidsgiverInntekt ->
                arbeidsgiverInntekt + acc
            }
        }
    }
}