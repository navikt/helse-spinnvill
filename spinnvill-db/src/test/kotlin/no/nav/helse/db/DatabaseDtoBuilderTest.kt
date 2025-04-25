package no.nav.helse.db

import no.nav.helse.*
import no.nav.helse.db.DatabaseDtoBuilder.Companion.tilDomene
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.test.assertContains
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
                        )
                    )
                )
            ),
            opprettet = LocalDateTime.now(),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(mapOf(
                Arbeidsgiverreferanse("987654321") to OmregnetÅrsinntekt(240000.0)
            ))
        )

        val avviksvurdering = avviksvurderingDto.tilDomene()

        val builder = DatabaseDtoBuilder()

        assertEquals(avviksvurderingDto, builder.buildAll(listOf(avviksvurdering)).single())
    }

    @Test
    fun `bygg en liste av avviksvurderinger`() {
        val skjæringstidspunkt = 1.januar
        val avviksvurderingDto1 = AvviksvurderingDto(
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
                        )
                    ),
                    Arbeidsgiverreferanse("123456789") to listOf(
                        AvviksvurderingDto.MånedligInntektDto(
                            inntekt = InntektPerMåned(30000.0),
                            måned = YearMonth.from(skjæringstidspunkt),
                            fordel = Fordel("En annen fordel"),
                            beskrivelse = Beskrivelse("En annen beskrivelse"),
                            inntektstype = AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT
                        )
                    )
                )
            ),
            opprettet = LocalDateTime.now(),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
                mapOf(
                    Arbeidsgiverreferanse("987654321") to OmregnetÅrsinntekt(240000.0),
                    Arbeidsgiverreferanse("123456789") to OmregnetÅrsinntekt(420000.0)
                )
            )
        )
        val avviksvurderingDto2 = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = Fødselsnummer("12345678910"),
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()),
            opprettet = LocalDateTime.now(),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
                mapOf(
                    Arbeidsgiverreferanse("987654321") to OmregnetÅrsinntekt(230000.0),
                    Arbeidsgiverreferanse("123456789") to OmregnetÅrsinntekt(430000.0)
                )
            )
        )
        val avviksvurderingDto3 = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = Fødselsnummer("12345678910"),
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(
                    Arbeidsgiverreferanse("987654321") to listOf(
                        AvviksvurderingDto.MånedligInntektDto(
                            inntekt = InntektPerMåned(60000.0),
                            måned = YearMonth.from(skjæringstidspunkt),
                            fordel = Fordel("En fordel"),
                            beskrivelse = Beskrivelse("En beskrivelse"),
                            inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                        )
                    ),
                    Arbeidsgiverreferanse("123456789") to listOf(
                        AvviksvurderingDto.MånedligInntektDto(
                            inntekt = InntektPerMåned(80000.0),
                            måned = YearMonth.from(skjæringstidspunkt),
                            fordel = Fordel("En annen fordel"),
                            beskrivelse = Beskrivelse("En annen beskrivelse"),
                            inntektstype = AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT
                        ),
                        AvviksvurderingDto.MånedligInntektDto(
                            inntekt = InntektPerMåned(20000.0),
                            måned = YearMonth.from(skjæringstidspunkt).minusMonths(1),
                            fordel = Fordel("En annen fordel"),
                            beskrivelse = Beskrivelse("En annen beskrivelse"),
                            inntektstype = AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT
                        )
                    )
                )
            ),
            opprettet = LocalDateTime.now(),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
                mapOf(
                    Arbeidsgiverreferanse("987654321") to OmregnetÅrsinntekt(780000.0),
                    Arbeidsgiverreferanse("123456789") to OmregnetÅrsinntekt(870000.0),
                    Arbeidsgiverreferanse("111111111") to OmregnetÅrsinntekt(300000.0),
                )
            )
        )

        val avviksvurdering1 = avviksvurderingDto1.tilDomene()
        val avviksvurdering2 = avviksvurderingDto2.tilDomene()
        val avviksvurdering3 = avviksvurderingDto3.tilDomene()

        val builder = DatabaseDtoBuilder()
        val avviksvurderinger = builder.buildAll(listOf(avviksvurdering1, avviksvurdering2, avviksvurdering3))
        assertContains(avviksvurderinger, avviksvurderingDto1)
        assertContains(avviksvurderinger, avviksvurderingDto2)
        assertContains(avviksvurderinger, avviksvurderingDto3)
    }
}
