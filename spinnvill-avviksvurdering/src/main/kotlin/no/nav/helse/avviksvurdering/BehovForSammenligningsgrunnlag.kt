package no.nav.helse.avviksvurdering

import java.time.LocalDate
import java.time.YearMonth

data class BehovForSammenligningsgrunnlag(
    val skjæringstidspunkt: LocalDate,
    val beregningsperiodeFom: YearMonth,
    val beregningsperiodeTom: YearMonth
)