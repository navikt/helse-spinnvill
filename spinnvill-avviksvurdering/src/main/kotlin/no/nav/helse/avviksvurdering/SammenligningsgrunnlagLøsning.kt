package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import java.time.LocalDate
import java.util.*

data class SammenligningsgrunnlagLøsning(
    val fødselsnummer: Fødselsnummer,
    val skjæringstidspunkt: LocalDate,
    val avviksvurderingBehovId: UUID,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
)