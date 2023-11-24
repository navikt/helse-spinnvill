package no.nav.helse.dto

import no.nav.helse.Fødselsnummer
import no.nav.helse.InntektPerMåned
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Organisasjonsnummer
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class AvviksvurderingDto(
    val id: UUID,
    val fødselsnummer: Fødselsnummer,
    val skjæringstidspunkt: LocalDate,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val beregningsgrunnlag: BeregningsgrunnlagDto?
) {
    data class SammenligningsgrunnlagDto(
        val innrapporterteInntekter: Map<Organisasjonsnummer, List<MånedligInntektDto>>
    )

    data class MånedligInntektDto(
        val inntekt: InntektPerMåned,
        val måned: YearMonth
    )

    data class BeregningsgrunnlagDto(
        val omregnedeÅrsinntekter: Map<Organisasjonsnummer, OmregnetÅrsinntekt>
    )
}