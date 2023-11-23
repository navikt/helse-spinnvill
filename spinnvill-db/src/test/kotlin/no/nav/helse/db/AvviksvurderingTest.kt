package no.nav.helse.db

import no.nav.helse.TestDatabase
import no.nav.helse.dto.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class AvviksvurderingTest {

    val database = TestDatabase.database()
    private val avviksvurdering = Avviksvurdering()

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
    }

    @Test
    fun `Kan lage en avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val sammenligningsgrunnlag = sammenligningsgrunnlag()

        val id = UUID.randomUUID()
        val avviksvurdering = avviksvurdering.upsert(id, fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag, null)

        assertEquals(id, avviksvurdering.id)
        assertEquals(fødselsnummer, avviksvurdering.fødselsnummer)
        assertEquals(skjæringstidspunkt, avviksvurdering.skjæringstidspunkt)
        assertEquals(sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key)
        assertEquals(sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value)
        assertNull(avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `Oppdater eksisterende avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val sammenligningsgrunnlag= sammenligningsgrunnlag()
        val beregningsgrunnlag = beregningsgrunnlag()

        val id = UUID.randomUUID()

        val nyAvviksvurdering = avviksvurdering.upsert(id, fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag, null)
        val modifisertAvviksvurdering = avviksvurdering.upsert(id, fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag, beregningsgrunnlag)

        assertEquals(nyAvviksvurdering.id, modifisertAvviksvurdering.id)
        assertEquals(beregningsgrunnlag.omregnedeÅrsinntekter.keys.first(), modifisertAvviksvurdering.beregningsgrunnlag?.omregnedeÅrsinntekter?.keys?.first())
        assertEquals(beregningsgrunnlag.omregnedeÅrsinntekter.values.first(), modifisertAvviksvurdering.beregningsgrunnlag?.omregnedeÅrsinntekter?.values?.first())
    }

    @Test
    fun `Finn siste avviksvurdering for fødselsnummer og skjæringstidspunkt`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar

        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag(20000.0), beregningsgrunnlag(200000.0))
        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag(40000.0), beregningsgrunnlag(300000.0))
        val expectedLatest = avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag(60000.0), beregningsgrunnlag(500000.0))
        val avviksvurdering = avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)

        assertNotNull(avviksvurdering)
        assertEquals(expectedLatest.id, avviksvurdering.id)
        assertEquals(expectedLatest.sammenligningsgrunnlag, avviksvurdering.sammenligningsgrunnlag)
        assertEquals(expectedLatest.beregningsgrunnlag, avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `Finn siste avviksvurdering når siste ikke har beregningsgrunnlag enda`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar

        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag(20000.0), beregningsgrunnlag(200000.0))
        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag(40000.0), beregningsgrunnlag(300000.0))
        val expectedLatest = avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag(60000.0), null)
        val avviksvurdering = avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)

        assertNotNull(avviksvurdering)
        assertEquals(expectedLatest.id, avviksvurdering.id)
        assertNull(avviksvurdering.beregningsgrunnlag)
    }

    private fun beregningsgrunnlag(omregnetÅrsinntekt: Double = 20000.0): AvviksvurderingDto.BeregningsgrunnlagDto {
        return AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(Organisasjonsnummer("123456789") to OmregnetÅrsinntekt(omregnetÅrsinntekt))
        )
    }

    private fun sammenligningsgrunnlag(inntektPerMåned: Double = 200000.0): AvviksvurderingDto.SammenligningsgrunnlagDto {
        return AvviksvurderingDto.SammenligningsgrunnlagDto(
            mapOf(Organisasjonsnummer("123456789") to listOf(
                AvviksvurderingDto.MånedligInntektDto(
                    InntektPerMåned(inntektPerMåned), YearMonth.of(2020, 1))
                )
            )
        )
    }
}