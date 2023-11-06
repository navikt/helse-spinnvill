package no.nav.helse.modell

import java.time.YearMonth

data class BehovForSammenligningsgrunnlag(
    val beregningsperiodeFom: YearMonth,
    val beregningsperiodeTom: YearMonth
)