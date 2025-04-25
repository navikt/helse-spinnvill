package no.nav.helse.avviksvurdering

import java.time.LocalDateTime
import java.util.*

data class Avviksvurdering(
    val id: UUID,
    val harAkseptabeltAvvik: Boolean,
    val avviksprosent: Double,
    val beregningsgrunnlag: Beregningsgrunnlag,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val maksimaltTillattAvvik: Double
) {
    val vurderingstidspunkt: LocalDateTime = LocalDateTime.now()
}

sealed interface Avviksvurderingsresultat {
    data class TrengerSammenligningsgrunnlag(val behov: BehovForSammenligningsgrunnlag) : Avviksvurderingsresultat
    data class AvvikVurdert(val vurdering: Avviksvurdering): Avviksvurderingsresultat
    data class TrengerIkkeNyVurdering(val gjeldendeGrunnlag: Avviksvurderingsgrunnlag) : Avviksvurderingsresultat
}
