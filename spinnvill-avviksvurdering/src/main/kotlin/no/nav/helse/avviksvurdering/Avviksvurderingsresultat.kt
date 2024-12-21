package no.nav.helse.avviksvurdering

import java.util.*

data class Vurdering(
    val id: UUID,
    val harAkseptabeltAvvik: Boolean,
    val avviksprosent: Double,
    val beregningsgrunnlag: Beregningsgrunnlag,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val maksimaltTillattAvvik: Double
)

sealed interface Avviksvurderingsresultat {
    data class TrengerSammenligningsgrunnlag(val behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) :
        Avviksvurderingsresultat
    data class AvvikVurdert(
        val avviksvurderingsgrunnlag: Avviksvurderingsgrunnlag,
        val vurdering: Vurdering,
    ): Avviksvurderingsresultat

    data class TrengerIkkeNyVurdering(val gjeldendeAvviksvurderingsgrunnlag: Avviksvurderingsgrunnlag) : Avviksvurderingsresultat
}
