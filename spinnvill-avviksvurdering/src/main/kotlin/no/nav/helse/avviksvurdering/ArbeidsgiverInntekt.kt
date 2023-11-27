package no.nav.helse.avviksvurdering

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Beskrivelse
import no.nav.helse.Fordel
import no.nav.helse.InntektPerMåned
import java.time.YearMonth

class ArbeidsgiverInntekt(
    private val arbeidsgiverreferanse: Arbeidsgiverreferanse,
    private val inntekter: List<MånedligInntekt>
) {
    operator fun plus(other: Double) = other + inntekter.sumOf { it.inntekt.value }

    internal companion object {
        internal fun Iterable<ArbeidsgiverInntekt>.sum(): Double {
            return this.fold(0.0) { acc, arbeidsgiverInntekt ->
                arbeidsgiverInntekt + acc
            }
        }
    }

    data class MånedligInntekt(
        val inntekt: InntektPerMåned,
        val måned: YearMonth,
        val fordel: Fordel?,
        val beskrivelse: Beskrivelse?,
        val inntektstype: Inntektstype
    )

    enum class Inntektstype {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE,
    }

    internal fun accept(visitor: Visitor) {
        visitor.visitArbeidsgiverInntekt(arbeidsgiverreferanse, inntekter)
    }

}