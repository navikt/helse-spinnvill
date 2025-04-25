package no.nav.helse.avviksvurdering

import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Arbeidsgiverreferanse
import kotlin.math.absoluteValue

data class Beregningsgrunnlag(val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>) {

    init {
        require(omregnedeÅrsinntekter.isNotEmpty()) { "Omregnede årsinntekter kan ikke være en tom liste"}
    }

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

    fun erLikt(other: Beregningsgrunnlag): Boolean {
        return !erOverGrenseForNyAvviksvurdering(other)
    }
}
