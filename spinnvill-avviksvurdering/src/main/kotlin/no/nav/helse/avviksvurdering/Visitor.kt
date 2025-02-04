package no.nav.helse.avviksvurdering

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface Visitor {
    fun visitAvviksvurderingsgrunnlag(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        kilde: Kilde,
        opprettet: LocalDateTime,
        beregningsgrunnlag: IBeregningsgrunnlag
    ) {}

    fun visitArbeidsgiverInntekt(arbeidsgiverreferanse: Arbeidsgiverreferanse, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>) {}
    fun visitSammenligningsgrunnlag(sammenligningsgrunnlag: Double) {}
}
