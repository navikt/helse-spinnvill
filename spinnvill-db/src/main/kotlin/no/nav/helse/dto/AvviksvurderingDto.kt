package no.nav.helse.dto

import no.nav.helse.*
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
        val måned: YearMonth,
        val fordel: Fordel?,
        val beskrivelse: Beskrivelse?,
        val inntektstype: InntektstypeDto
    )

    data class BeregningsgrunnlagDto(
        val omregnedeÅrsinntekter: Map<Organisasjonsnummer, OmregnetÅrsinntekt>
    )

    enum class InntektstypeDto {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE,
    }
}