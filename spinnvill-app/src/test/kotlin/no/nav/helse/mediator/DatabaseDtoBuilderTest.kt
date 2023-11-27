package no.nav.helse.mediator

import no.nav.helse.*
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.helpers.januar
import no.nav.helse.mediator.Mediator.Companion.tilDomene
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals

class DatabaseDtoBuilderTest {
    @Test
    fun `bygg en fullstendig avviksvurdering dto`() {
        val skjæringstidspunkt = 1.januar
        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = Fødselsnummer("12345678910"),
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(
                    Arbeidsgiverreferanse("987654321") to listOf(
                        AvviksvurderingDto.MånedligInntektDto(
                            inntekt = InntektPerMåned(20000.0),
                            måned = YearMonth.from(skjæringstidspunkt),
                            fordel = Fordel("En fordel"),
                            beskrivelse = Beskrivelse("En beskrivelse"),
                            inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                        )))
            ),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(mapOf(
                Arbeidsgiverreferanse("987654321") to OmregnetÅrsinntekt(240000.0)
            ))
        )

        val avviksvurdering = avviksvurderingDto.tilDomene()

        val builder = DatabaseDtoBuilder()
        avviksvurdering.accept(builder)

        assertEquals(avviksvurderingDto, builder.build())
    }

    @Test
    fun `bygg en ufullstendig avviksvurdering dto`() {
        val skjæringstidspunkt = 1.januar
        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = Fødselsnummer("12345678910"),
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(
                    Arbeidsgiverreferanse("987654321") to listOf(
                        AvviksvurderingDto.MånedligInntektDto(
                            inntekt = InntektPerMåned(20000.0),
                            måned = YearMonth.from(skjæringstidspunkt),
                            fordel = Fordel("En fordel"),
                            beskrivelse = Beskrivelse("En beskrivelse"),
                            inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                        )))
            ),
            beregningsgrunnlag = null
        )

        val avviksvurdering = avviksvurderingDto.tilDomene()

        val builder = DatabaseDtoBuilder()
        avviksvurdering.accept(builder)

        assertEquals(avviksvurderingDto, builder.build())
    }
}