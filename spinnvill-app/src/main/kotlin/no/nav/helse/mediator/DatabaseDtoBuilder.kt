package no.nav.helse.mediator

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.*
import no.nav.helse.dto.AvviksvurderingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DatabaseDtoBuilder : Visitor {

    private val avviksvurderinger = mutableListOf<AvviksvurderingDto>()
    private val gjeldende get() = avviksvurderinger.last()

    override fun visitAvviksvurderingsgrunnlag(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        kilde: Kilde,
        opprettet: LocalDateTime,
        beregningsgrunnlag: IBeregningsgrunnlag
    ) {
        avviksvurderinger.add(
            AvviksvurderingDto(
                id = id,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                kilde = kilde.tilDto(),
                sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()),
                beregningsgrunnlag =
                    if (beregningsgrunnlag is Beregningsgrunnlag) AvviksvurderingDto.BeregningsgrunnlagDto(beregningsgrunnlag.omregnedeÅrsinntekter)
                    else null
            )
        )
    }

    override fun visitArbeidsgiverInntekt(arbeidsgiverreferanse: Arbeidsgiverreferanse, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>) {
        val innrapporterteInntekterCopy = gjeldende.sammenligningsgrunnlag.innrapporterteInntekter.toMutableMap()
        innrapporterteInntekterCopy[arbeidsgiverreferanse] = inntekter.map { månedligInntekt ->
            AvviksvurderingDto.MånedligInntektDto(
                inntekt = månedligInntekt.inntekt,
                måned = månedligInntekt.måned,
                fordel = månedligInntekt.fordel,
                beskrivelse = månedligInntekt.beskrivelse,
                inntektstype = månedligInntekt.inntektstype.tilDto()
            )
        }
        val nyGjeldende = gjeldende.copy(
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(innrapporterteInntekterCopy.toMap())
        )
        erstattGjeldendeMed(nyGjeldende)
    }

    private fun erstattGjeldendeMed(nyGjeldende: AvviksvurderingDto) {
        val index = avviksvurderinger.indexOf(gjeldende)
        avviksvurderinger[index] = nyGjeldende
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

    internal fun build(): AvviksvurderingDto = avviksvurderinger.single()

    internal fun buildAll(): List<AvviksvurderingDto> = avviksvurderinger
}
