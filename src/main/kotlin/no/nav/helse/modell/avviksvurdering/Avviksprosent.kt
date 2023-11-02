package no.nav.helse.modell.avviksvurdering

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

internal class Avviksprosent private constructor(private val avviksbrøk: Double): Comparable<Avviksprosent> {

    init {
        require(avviksbrøk in 0.0..1.0) {
            "Avviksbrøken må være en verdi mellom 0.0 og 1.0"
        }
    }

    internal fun harAkseptabeltAvvik(): Boolean {
        return this <= MAKSIMALT_TILLATT_AVVIK
    }
    internal companion object {
        private const val EPSILON = 0.000001
        private val MAKSIMALT_TILLATT_AVVIK = Avviksprosent(0.25)
        internal fun avvik(beregningsgrunnlag: Double, sammenligningsgrunnlag: Double): Avviksprosent {
            val avviksbrøk = if(sammenligningsgrunnlag == 0.0) 1.0
            else (beregningsgrunnlag - sammenligningsgrunnlag).absoluteValue / sammenligningsgrunnlag
            return Avviksprosent(avviksbrøk)
        }
    }

    override fun compareTo(other: Avviksprosent) =
        if (this == other) 0
        else this.avviksbrøk.compareTo(other.avviksbrøk)

    override fun equals(other: Any?) = other is Avviksprosent && (this.avviksbrøk - other.avviksbrøk).absoluteValue < EPSILON

    override fun hashCode() = (avviksbrøk / EPSILON).roundToLong().hashCode()
}