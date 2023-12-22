package no.nav.helse.mediator

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Kilde
import no.nav.helse.avviksvurdering.Visitor
import no.nav.helse.dto.AvviksvurderingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DatabaseDtoBuilder : Visitor {

    private lateinit var id: UUID
    private lateinit var fødselsnummer: String
    private lateinit var skjæringstidspunkt: LocalDate
    private lateinit var omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>
    private lateinit var opprettet: LocalDateTime
    private lateinit var kilde: Kilde
    private val innrapporterteInntekter: MutableMap<Arbeidsgiverreferanse, List<AvviksvurderingDto.MånedligInntektDto>> = mutableMapOf()

    override fun visitAvviksvurdering(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        kilde: Kilde,
        opprettet: LocalDateTime
    ) {
        this.id = id
        this.fødselsnummer = fødselsnummer.value
        this.skjæringstidspunkt = skjæringstidspunkt
        this.opprettet = opprettet
        this.kilde = kilde
    }

    override fun visitBeregningsgrunnlag(
        totaltOmregnetÅrsinntekt: Double,
        omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>
    ) {
        this.omregnedeÅrsinntekter = omregnedeÅrsinntekter
    }

    override fun visitArbeidsgiverInntekt(arbeidsgiverreferanse: Arbeidsgiverreferanse, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>) {
        innrapporterteInntekter[arbeidsgiverreferanse] = inntekter.map {
            AvviksvurderingDto.MånedligInntektDto(
                inntekt = it.inntekt,
                måned = it.måned,
                fordel = it.fordel,
                beskrivelse = it.beskrivelse,
                inntektstype = it.inntektstype.tilDto()
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

    internal fun build(): AvviksvurderingDto {
        return AvviksvurderingDto(
            id = id,
            fødselsnummer = Fødselsnummer(fødselsnummer),
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(innrapporterteInntekter = innrapporterteInntekter),
            opprettet = opprettet,
            kilde = kilde.tilDto(),
            beregningsgrunnlag = omregnedeÅrsinntekter
                .takeUnless { it.isEmpty() }
                ?.let { AvviksvurderingDto.BeregningsgrunnlagDto(omregnedeÅrsinntekter = it) }
        )
    }
}