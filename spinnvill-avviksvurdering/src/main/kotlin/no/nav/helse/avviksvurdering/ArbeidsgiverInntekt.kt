package no.nav.helse.avviksvurdering

class ArbeidsgiverInntekt(private val organisasjonsummer: String, private val inntekter: List<Double>) {

    operator fun plus(other: Double) = other + inntekter.sum()

    internal companion object {
        internal fun Iterable<ArbeidsgiverInntekt>.sum(): Double {
            return this.fold(0.0) { acc, arbeidsgiverInntekt ->
                arbeidsgiverInntekt + acc
            }
        }
    }
}