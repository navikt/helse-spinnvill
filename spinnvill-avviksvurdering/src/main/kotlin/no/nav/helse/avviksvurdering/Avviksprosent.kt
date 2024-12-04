package no.nav.helse.avviksvurdering

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal class Avviksprosent private constructor(private val prosent: Double): Comparable<Avviksprosent> {

    internal fun harAkseptabeltAvvik(): Boolean = this <= MAKSIMALT_TILLATT_AVVIK

    internal fun avrundetTilFireDesimaler(): Double = (prosent * PRESISJON).roundToInt() / PRESISJON

    internal companion object {
        private const val PRESISJON = 10000.0
        private const val EPSILON = 1 / PRESISJON
        internal val MAKSIMALT_TILLATT_AVVIK = Avviksprosent(25.0)

        internal fun avvik(beregningsgrunnlag: Double, sammenligningsgrunnlag: Double): Avviksprosent {
            val avviksprosent =
                if (sammenligningsgrunnlag == 0.0) 100.0
                else ((beregningsgrunnlag - sammenligningsgrunnlag).absoluteValue / sammenligningsgrunnlag) * 100
            return Avviksprosent(avviksprosent)
        }

        internal val INGEN = Avviksprosent(-1.0)
    }

    override fun toString(): String = "$prosent"

    override fun compareTo(other: Avviksprosent) =
        if (this == other) 0
        else this.avrundetTilFireDesimaler().compareTo(other.avrundetTilFireDesimaler())

    override fun equals(other: Any?) = other is Avviksprosent && (this.prosent - other.prosent).absoluteValue < EPSILON

    override fun hashCode() = (prosent / EPSILON).roundToLong().hashCode()
}