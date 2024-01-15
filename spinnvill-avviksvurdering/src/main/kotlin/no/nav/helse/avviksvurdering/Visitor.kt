package no.nav.helse.avviksvurdering

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.OmregnetÅrsinntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface Visitor {
    fun visitAvviksvurdering(id: UUID, fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate, kilde: Kilde, opprettet: LocalDateTime) {}
    fun visitBeregningsgrunnlag(totaltOmregnetÅrsinntekt: Double, omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>) {}
    fun visitBeregningsgrunnlagIngen() {}
    fun visitArbeidsgiverInntekt(arbeidsgiverreferanse: Arbeidsgiverreferanse, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>) {}
    fun visitSammenligningsgrunnlag(sammenligningsgrunnlag: Double) {}
}