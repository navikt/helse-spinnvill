package no.nav.helse.mediator

import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Avviksvurderingsgrunnlag
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Kilde
import no.nav.helse.dto.AvviksvurderingDto

class DatabaseDtoBuilder {

    internal fun buildAll(grunnlagene: List<Avviksvurderingsgrunnlag>): List<AvviksvurderingDto> = grunnlagene.toDto()

    private fun Collection<Avviksvurderingsgrunnlag>.toDto() = map { grunnlag ->
        val innrapporterteInntekter = grunnlag.sammenligningsgrunnlag.inntekter.associate { it.arbeidsgiverreferanse to it.inntekter.toDto() }

        val beregningsgrunnlag = grunnlag.beregningsgrunnlag
        AvviksvurderingDto(
            id = grunnlag.id,
            fødselsnummer = grunnlag.fødselsnummer,
            skjæringstidspunkt = grunnlag.skjæringstidspunkt,
            opprettet = grunnlag.opprettet,
            kilde = grunnlag.kilde.tilDto(),
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(innrapporterteInntekter),
            beregningsgrunnlag =
                if (beregningsgrunnlag is Beregningsgrunnlag) AvviksvurderingDto.BeregningsgrunnlagDto(beregningsgrunnlag.omregnedeÅrsinntekter)
                else null
        )
    }

    private fun List<ArbeidsgiverInntekt.MånedligInntekt>.toDto(): List<AvviksvurderingDto.MånedligInntektDto> {
        return map { månedligInntekt ->
            AvviksvurderingDto.MånedligInntektDto(
                inntekt = månedligInntekt.inntekt,
                måned = månedligInntekt.måned,
                fordel = månedligInntekt.fordel,
                beskrivelse = månedligInntekt.beskrivelse,
                inntektstype = månedligInntekt.inntektstype.tilDto()
            )
        }
    }

    private fun ArbeidsgiverInntekt.Inntektstype.tilDto(): AvviksvurderingDto.InntektstypeDto {
        return when (this) {
            ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT -> AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
            ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT -> AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT
            ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD -> AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD
            ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE -> AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE
        }
    }

    private fun Kilde.tilDto() = when (this) {
        Kilde.SPLEIS -> AvviksvurderingDto.KildeDto.SPLEIS
        Kilde.SPINNVILL -> AvviksvurderingDto.KildeDto.SPINNVILL
        Kilde.INFOTRYGD -> AvviksvurderingDto.KildeDto.INFOTRYGD
    }
}
