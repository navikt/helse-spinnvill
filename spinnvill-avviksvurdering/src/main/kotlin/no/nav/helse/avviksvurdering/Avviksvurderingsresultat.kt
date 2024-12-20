package no.nav.helse.avviksvurdering

import java.util.*

sealed interface Avviksvurderingsresultat {
    data class TrengerSammenligningsgrunnlag(val behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) :
        Avviksvurderingsresultat
    data class AvvikVurdert(
        val avviksvurdering: Avviksvurdering,
        val id: UUID,
        val harAkseptabeltAvvik: Boolean,
        val avviksprosent: Double,
        val beregningsgrunnlag: Beregningsgrunnlag,
        val sammenligningsgrunnlag: Sammenligningsgrunnlag,
        val maksimaltTillattAvvik: Double
    ): Avviksvurderingsresultat

    data class TrengerIkkeNyVurdering(val gjeldendeAvviksvurdering: Avviksvurdering) : Avviksvurderingsresultat
}
