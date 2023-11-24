package no.nav.helse.avviksvurdering

import no.nav.helse.InntektPerMåned
import no.nav.helse.Organisasjonsnummer
import java.time.YearMonth

class ArbeidsgiverInntekt(private val organisasjonsummer: Organisasjonsnummer, private val inntekter: Map<YearMonth, InntektPerMåned>) {

    operator fun plus(other: Double) = other + inntekter.values.sumOf { it.value }

    internal companion object {
        internal fun Iterable<ArbeidsgiverInntekt>.sum(): Double {
            return this.fold(0.0) { acc, arbeidsgiverInntekt ->
                arbeidsgiverInntekt + acc
            }
        }
    }
}