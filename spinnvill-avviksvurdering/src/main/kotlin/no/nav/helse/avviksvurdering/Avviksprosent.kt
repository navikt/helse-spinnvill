package no.nav.helse.avviksvurdering

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Avviksprosent internal constructor(prosent: Double): Comparable<Avviksprosent> {

    internal val avrundetTilFireDesimaler = (prosent * PRESISJON).roundToInt() / PRESISJON

    override fun toString(): String = "$avrundetTilFireDesimaler %"

    override fun compareTo(other: Avviksprosent) =
        if (this == other) 0
        else this.avrundetTilFireDesimaler.compareTo(other.avrundetTilFireDesimaler)

    override fun equals(other: Any?) =
        other is Avviksprosent && (this.avrundetTilFireDesimaler == other.avrundetTilFireDesimaler)

    override fun hashCode() = avrundetTilFireDesimaler.hashCode()

    internal companion object {
        private const val PRESISJON = 10000.0
        internal val INGEN = Avviksprosent(-1.0)

        internal fun avvik(beregningsgrunnlag: Double, sammenligningsgrunnlag: Double) =
            if (sammenligningsgrunnlag == 0.0) {
                Avviksprosent(100.0)
            } else {
                Avviksprosent(((beregningsgrunnlag - sammenligningsgrunnlag).absoluteValue / sammenligningsgrunnlag) * 100)
            }
    }
}
