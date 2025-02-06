package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import java.time.LocalDate
import java.util.*

data class AvviksvurderingBehov(
    val vilkårsgrunnlagId: UUID,
    val behovId: UUID,
    val skjæringstidspunkt: LocalDate,
    val fødselsnummer: Fødselsnummer,
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val beregningsgrunnlag : Beregningsgrunnlag
)