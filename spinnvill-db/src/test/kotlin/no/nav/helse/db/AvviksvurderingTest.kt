package no.nav.helse.db

import no.nav.helse.*
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.dto.AvviksvurderingDto.KildeDto.SPINNVILL
import no.nav.helse.helpers.januar
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
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
        val avviksvurdering = avviksvurdering.upsert(id, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, null)

        assertEquals(id, avviksvurdering.id)
        assertEquals(fødselsnummer, avviksvurdering.fødselsnummer)
        assertEquals(skjæringstidspunkt, avviksvurdering.skjæringstidspunkt)
        assertEquals(SPINNVILL, avviksvurdering.kilde)
        assertEquals(sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key)
        assertEquals(sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value)
        assertNull(avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `Oppdater eksisterende avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val sammenligningsgrunnlag = sammenligningsgrunnlag()
        val beregningsgrunnlag = beregningsgrunnlag()

        val id = UUID.randomUUID()

        val nyAvviksvurdering = avviksvurdering.upsert(id, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, null)
        val modifisertAvviksvurdering = avviksvurdering.upsert(id, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)

        assertEquals(nyAvviksvurdering.id, modifisertAvviksvurdering.id)
        assertEquals(beregningsgrunnlag.omregnedeÅrsinntekter.keys.first(), modifisertAvviksvurdering.beregningsgrunnlag?.omregnedeÅrsinntekter?.keys?.first())
        assertEquals(beregningsgrunnlag.omregnedeÅrsinntekter.values.first(), modifisertAvviksvurdering.beregningsgrunnlag?.omregnedeÅrsinntekter?.values?.first())
    }

    @Test
    fun `Finn siste avviksvurdering for fødselsnummer og skjæringstidspunkt`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar

        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(2), sammenligningsgrunnlag(20000.0), beregningsgrunnlag(200000.0))
        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag(40000.0), beregningsgrunnlag(300000.0))
        val expectedLatest = avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag(60000.0), beregningsgrunnlag(500000.0))
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

        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(2), sammenligningsgrunnlag(20000.0), beregningsgrunnlag(200000.0))
        avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag(40000.0), beregningsgrunnlag(300000.0))
        val expectedLatest = avviksvurdering.upsert(UUID.randomUUID(), fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag(60000.0), null)
        val avviksvurdering = avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)

        assertNotNull(avviksvurdering)
        assertEquals(expectedLatest.id, avviksvurdering.id)
        assertNull(avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `oppretter ikke nytt beregningsgrunnlag om det samme finnes fra før`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag = beregningsgrunnlag(200000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        avviksvurdering.upsert(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag, beregningsgrunnlag)
        avviksvurdering.upsert(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)

        val antallBeregningsgrunnlag = transaction {
            Avviksvurdering.Companion.EttBeregningsgrunnlag.find { Avviksvurdering.Companion.Beregningsgrunnlag.avviksvurdering eq avviksvurderingId}.count()
        }

        assertEquals(1, antallBeregningsgrunnlag)
    }

    @Test
    fun `oppretter ikke nytt sammenligningsgrunnlag om det samme finnes fra før`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag = beregningsgrunnlag(200000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        avviksvurdering.upsert(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag, beregningsgrunnlag)
        avviksvurdering.upsert(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)

        val antallSammenligningsgrunnlag = transaction {
            Avviksvurdering.Companion.EttSammenligningsgrunnlag.find { Avviksvurdering.Companion.Sammenligningsgrunnlag.avviksvurdering eq avviksvurderingId}.count()
        }

        assertEquals(1, antallSammenligningsgrunnlag)
    }

    @ParameterizedTest
    @EnumSource(value = AvviksvurderingDto.KildeDto::class)
    fun `kan lagre avviksvurdering med alle kilder`(kildeDto: AvviksvurderingDto.KildeDto) {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag = beregningsgrunnlag(200000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        avviksvurdering.upsert(avviksvurderingId, fødselsnummer, skjæringstidspunkt, kildeDto, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)
        val avviksvurdering = avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)
        assertEquals(kildeDto, avviksvurdering?.kilde)
    }

    private fun beregningsgrunnlag(omregnetÅrsinntekt: Double = 20000.0): AvviksvurderingDto.BeregningsgrunnlagDto {
        return AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(Arbeidsgiverreferanse("123456789") to OmregnetÅrsinntekt(omregnetÅrsinntekt))
        )
    }

    private fun sammenligningsgrunnlag(inntektPerMåned: Double = 200000.0): AvviksvurderingDto.SammenligningsgrunnlagDto {
        return AvviksvurderingDto.SammenligningsgrunnlagDto(
            mapOf(
                Arbeidsgiverreferanse("123456789") to listOf(
                    AvviksvurderingDto.MånedligInntektDto(
                        InntektPerMåned(inntektPerMåned),
                        YearMonth.of(2020, 1),
                        fordel = Fordel("En fordel"),
                        beskrivelse = Beskrivelse("En beskrivelse"),
                        inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                    )
                )
            )
        )
    }
}