package no.nav.helse.dto

import no.nav.helse.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class AvviksvurderingDto(
    val id: UUID,
    val fødselsnummer: Fødselsnummer,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val kilde: KildeDto = KildeDto.SPINNVILL,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val beregningsgrunnlag: BeregningsgrunnlagDto?
) {
    data class SammenligningsgrunnlagDto(
        val innrapporterteInntekter: Map<Arbeidsgiverreferanse, List<MånedligInntektDto>>
    )

    data class MånedligInntektDto(
        val inntekt: InntektPerMåned,
        val måned: YearMonth,
        val fordel: Fordel?,
        val beskrivelse: Beskrivelse?,
        val inntektstype: InntektstypeDto
    )

    data class BeregningsgrunnlagDto(
        val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>
    )

    enum class InntektstypeDto {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE,
    }

    enum class KildeDto {
        SPINNVILL,
        SPLEIS,
        INFOTRYGD,
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is AvviksvurderingDto
            && id == other.id
            && fødselsnummer == other.fødselsnummer
            && skjæringstidspunkt == other.skjæringstidspunkt
            && opprettet.withNano(0) == other.opprettet.withNano(0)
            && kilde == other.kilde
            && sammenligningsgrunnlag == other.sammenligningsgrunnlag
            && beregningsgrunnlag == other.beregningsgrunnlag)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fødselsnummer.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + kilde.hashCode()
        result = 31 * result + sammenligningsgrunnlag.hashCode()
        result = 31 * result + (beregningsgrunnlag?.hashCode() ?: 0)
        return result
    }
}