package no.nav.helse.dto

import java.time.LocalDate
import java.util.UUID

data class AvviksvurderingDto(
    val id: UUID,
    val fødselsnummer: Fødselsnummer,
    val skjæringstidspunkt: LocalDate,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val beregningsgrunnlag: BeregningsgrunnlagDto?
) {
    data class SammenligningsgrunnlagDto(
        val innrapporterteInntekter: Map<Organisasjonsnummer, Map<InntektPerMåned, Pair<Måned, År>>>
    )

    data class BeregningsgrunnlagDto(
        val omregnedeÅrsinntekter: Map<Organisasjonsnummer, OmregnetÅrsinntekt>
    )
}