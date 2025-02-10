package no.nav.helse.avviksvurdering

import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Arbeidsgiverreferanse
import kotlin.math.absoluteValue

interface IBeregningsgrunnlag{
    fun erLikt(other: IBeregningsgrunnlag): Boolean
}

object Ingen: IBeregningsgrunnlag {
    override fun erLikt(other: IBeregningsgrunnlag): Boolean {
        return other is Ingen
    }
}
class Beregningsgrunnlag private constructor(val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>): IBeregningsgrunnlag {

    val totalOmregnetÅrsinntekt = omregnedeÅrsinntekter.values.sumOf { it.value }
    private val GRENSE_FOR_NY_AVVIKSVURDERING = 1.0
    internal fun beregnAvvik(sammenligningsgrunnlag: Double): Avviksprosent {
        return Avviksprosent.avvik(
            beregningsgrunnlag = totalOmregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )
    }

    private fun erOverGrenseForNyAvviksvurdering(other: Beregningsgrunnlag): Boolean {
        return (this.totalOmregnetÅrsinntekt - other.totalOmregnetÅrsinntekt).absoluteValue >= GRENSE_FOR_NY_AVVIKSVURDERING
    }

    override fun erLikt(other: IBeregningsgrunnlag): Boolean {
        return other is Beregningsgrunnlag && !erOverGrenseForNyAvviksvurdering(other)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beregningsgrunnlag

        return omregnedeÅrsinntekter == other.omregnedeÅrsinntekter
    }

    override fun hashCode(): Int {
        return omregnedeÅrsinntekter.hashCode()
    }

    companion object {
        fun opprett(omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>) : Beregningsgrunnlag {
            require(omregnedeÅrsinntekter.isNotEmpty()) { "Omregnede årsinntekter kan ikke være en tom liste"}
            return Beregningsgrunnlag(omregnedeÅrsinntekter)
        }
    }
}
