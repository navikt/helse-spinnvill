package no.nav.helse.avviksvurdering

import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Arbeidsgiverreferanse
import kotlin.math.absoluteValue

class Beregningsgrunnlag private constructor(private val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>) {

    private val totalOmregnetÅrsinntekt = omregnedeÅrsinntekter.values.sumOf { it.value }
    private val GRENSE_FOR_NY_AVVIKSVURDERING = 1.0
    internal fun beregnAvvik(sammenligningsgrunnlag: Double): Avviksprosent {
        return Avviksprosent.avvik(
            beregningsgrunnlag = totalOmregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )
    }

    internal fun erOverGrenseForNyAvviksvurdering(other: Beregningsgrunnlag): Boolean {
        return (this.totalOmregnetÅrsinntekt - other.totalOmregnetÅrsinntekt).absoluteValue >= GRENSE_FOR_NY_AVVIKSVURDERING
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

    fun accept(visitor: Visitor) {
        visitor.visitBeregningsgrunnlag(totalOmregnetÅrsinntekt, omregnedeÅrsinntekter)
    }

    companion object {
        val INGEN = Beregningsgrunnlag(emptyMap())
        fun opprett(omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>) : Beregningsgrunnlag {
            require(omregnedeÅrsinntekter.isNotEmpty()) { "Omregnede årsinntekter kan ikke være en tom liste"}
            return Beregningsgrunnlag(omregnedeÅrsinntekter)
        }
    }
}