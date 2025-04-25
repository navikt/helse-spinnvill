package no.nav.helse.db

import no.nav.helse.avviksvurdering.*
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
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(beregningsgrunnlag.omregnedeÅrsinntekter)
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

    internal companion object {
        internal fun AvviksvurderingDto.tilDomene(): Avviksvurderingsgrunnlag {
            return Avviksvurderingsgrunnlag(
                id = id,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlag = Beregningsgrunnlag(beregningsgrunnlag.omregnedeÅrsinntekter),
                opprettet = opprettet,
                kilde = this.kilde.tilDomene(),
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    sammenligningsgrunnlag.innrapporterteInntekter.map { (organisasjonsnummer, inntekter) ->
                        ArbeidsgiverInntekt(
                            arbeidsgiverreferanse = organisasjonsnummer,
                            inntekter = inntekter.map {
                                ArbeidsgiverInntekt.MånedligInntekt(
                                    inntekt = it.inntekt,
                                    måned = it.måned,
                                    fordel = it.fordel,
                                    beskrivelse = it.beskrivelse,
                                    inntektstype = it.inntektstype.tilDomene()
                                )
                            }
                        )
                    }
                )
            )
        }

        private fun AvviksvurderingDto.InntektstypeDto.tilDomene(): ArbeidsgiverInntekt.Inntektstype {
            return when (this) {
                AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT
                AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD -> ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD
                AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE -> ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE
            }
        }

        private fun AvviksvurderingDto.KildeDto.tilDomene() = when (this) {
            AvviksvurderingDto.KildeDto.SPINNVILL -> Kilde.SPINNVILL
            AvviksvurderingDto.KildeDto.SPLEIS -> Kilde.SPLEIS
            AvviksvurderingDto.KildeDto.INFOTRYGD -> Kilde.INFOTRYGD
        }
    }
}
