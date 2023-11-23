package no.nav.helse.avviksvurdering

import java.time.YearMonth

data class BehovForSammenligningsgrunnlag(
    val beregningsperiodeFom: YearMonth,
    val beregningsperiodeTom: YearMonth
)