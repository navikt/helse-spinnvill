package no.nav.helse.avviksvurdering

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.OmregnetÅrsinntekt
import java.time.LocalDate
import java.util.*

interface Visitor {
    fun visitAvviksvurdering(id: UUID, fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate) {}
    fun visitBeregningsgrunnlag(totaltOmregnetÅrsinntekt: Double, omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>) {}
    fun visitArbeidsgiverInntekt(arbeidsgiverreferanse: Arbeidsgiverreferanse, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>) {}
    fun visitSammenligningsgrunnlag(sammenligningsgrunnlag: Double) {}
}