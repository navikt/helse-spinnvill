package no.nav.helse.avviksvurdering

import no.nav.helse.Organisasjonsnummer

class ArbeidsgiverInntekt(private val organisasjonsummer: Organisasjonsnummer, private val inntekter: List<Double>) {

    operator fun plus(other: Double) = other + inntekter.sum()

    internal companion object {
        internal fun Iterable<ArbeidsgiverInntekt>.sum(): Double {
            return this.fold(0.0) { acc, arbeidsgiverInntekt ->
                arbeidsgiverInntekt + acc
            }
        }
    }
}