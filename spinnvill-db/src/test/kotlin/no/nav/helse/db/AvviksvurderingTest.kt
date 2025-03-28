package no.nav.helse.db

import no.nav.helse.*
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.dto.AvviksvurderingDto.KildeDto.SPINNVILL
import no.nav.helse.helpers.februar
import no.nav.helse.helpers.januar
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

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
        val avviksvurdering = opprettEn(id, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, null)

        assertEquals(id, avviksvurdering.id)
        assertEquals(fødselsnummer, avviksvurdering.fødselsnummer)
        assertEquals(skjæringstidspunkt, avviksvurdering.skjæringstidspunkt)
        assertEquals(SPINNVILL, avviksvurdering.kilde)
        assertEquals(sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key)
        assertEquals(sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value)
        assertNull(avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `Kan lage flere avviksvurderinger samtidig`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val sammenligningsgrunnlag = sammenligningsgrunnlag()

        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val avviksvurdering1 = AvviksvurderingDto(id1, fødselsnummer, skjæringstidspunkt, LocalDateTime.now(), SPINNVILL, sammenligningsgrunnlag, null)
        val avviksvurdering2 = AvviksvurderingDto(id2, fødselsnummer, skjæringstidspunkt, LocalDateTime.now(), SPINNVILL, sammenligningsgrunnlag, null)
        avviksvurdering.upsertAll(listOf(avviksvurdering1, avviksvurdering2))

        val avviksvurderinger = avviksvurdering.findAll(fødselsnummer, skjæringstidspunkt)

        assertEquals(2, avviksvurderinger.size)
        assertTrue(avviksvurderinger.contains(avviksvurdering1))
        assertTrue(avviksvurderinger.contains(avviksvurdering2))
    }

    @Test
    fun `Kan oppdatere flere avviksvurderinger samtidig`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar

        val id1 = UUID.randomUUID()
        val nyAvviksvurdering = AvviksvurderingDto(id1, fødselsnummer, skjæringstidspunkt, LocalDateTime.now(), SPINNVILL, sammenligningsgrunnlag(), null)
        val oppdatertAvviksvurdering = AvviksvurderingDto(id1, fødselsnummer, skjæringstidspunkt, LocalDateTime.now(), SPINNVILL, sammenligningsgrunnlag(), beregningsgrunnlag())
        avviksvurdering.upsertAll(listOf(nyAvviksvurdering))
        avviksvurdering.upsertAll(listOf(oppdatertAvviksvurdering))

        val avviksvurderinger = avviksvurdering.findAll(fødselsnummer, skjæringstidspunkt)

        assertEquals(1, avviksvurderinger.size)
        val avviksvurderingen = avviksvurderinger.single()
        assertEquals(nyAvviksvurdering.id, avviksvurderingen.id)
        assertEquals(oppdatertAvviksvurdering.id, avviksvurderingen.id)
        assertNotEquals(nyAvviksvurdering.beregningsgrunnlag, avviksvurderingen.beregningsgrunnlag)
        assertEquals(oppdatertAvviksvurdering.beregningsgrunnlag, avviksvurderingen.beregningsgrunnlag)
    }

    @Test
    fun `Oppdater eksisterende avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val sammenligningsgrunnlag = sammenligningsgrunnlag()
        val beregningsgrunnlag = beregningsgrunnlag()

        val id = UUID.randomUUID()

        val nyAvviksvurdering = opprettEn(id, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, null)
        val modifisertAvviksvurdering = opprettEn(id, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)

        assertEquals(nyAvviksvurdering.id, modifisertAvviksvurdering.id)
        assertEquals(beregningsgrunnlag.omregnedeÅrsinntekter.keys.first(), modifisertAvviksvurdering.beregningsgrunnlag?.omregnedeÅrsinntekter?.keys?.first())
        assertEquals(beregningsgrunnlag.omregnedeÅrsinntekter.values.first(), modifisertAvviksvurdering.beregningsgrunnlag?.omregnedeÅrsinntekter?.values?.first())
    }

    @Test
    fun `oppretter ikke nytt beregningsgrunnlag om det samme finnes fra før`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag = beregningsgrunnlag(200000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag, beregningsgrunnlag)
        opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)

        val antallBeregningsgrunnlag = transaction {
            Avviksvurdering.Companion.EttBeregningsgrunnlag.find { Avviksvurdering.Companion.Beregningsgrunnlag.avviksvurdering eq avviksvurderingId}.count()
        }

        assertEquals(1, antallBeregningsgrunnlag)
    }

    @Test
    fun `lagrer ikke duplikate beregningsgrunnlag`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag1 = beregningsgrunnlag(200000.001)
        val beregningsgrunnlag2 = beregningsgrunnlag(300000.05)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag, beregningsgrunnlag1)
        val upsert = opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag2)

        val antallBeregningsgrunnlag = transaction {
            Avviksvurdering.Companion.EttBeregningsgrunnlag.find { Avviksvurdering.Companion.Beregningsgrunnlag.avviksvurdering eq avviksvurderingId}.count()
        }

        assertEquals(beregningsgrunnlag1, upsert.beregningsgrunnlag)
        assertEquals(1, antallBeregningsgrunnlag)
    }

    @Test
    fun `oppretter ikke nytt sammenligningsgrunnlag om det samme finnes fra før`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag = beregningsgrunnlag(200000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now().minusDays(1), sammenligningsgrunnlag, beregningsgrunnlag)
        opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, SPINNVILL, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)

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

        opprettEn(avviksvurderingId, fødselsnummer, skjæringstidspunkt, kildeDto, LocalDateTime.now(), sammenligningsgrunnlag, beregningsgrunnlag)
        val avviksvurdering = avviksvurdering.findAll(fødselsnummer, skjæringstidspunkt).singleOrNull()
        assertEquals(kildeDto, avviksvurdering?.kilde)
    }

    @Test
    fun `henter liste av avviksvurderinger`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val avviksvurdering1 = opprettEn(UUID.randomUUID(), fødselsnummer, 1.januar, SPINNVILL, LocalDateTime.now(), AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()), null)
        val avviksvurdering2 = opprettEn(UUID.randomUUID(), fødselsnummer, 1.januar, SPINNVILL, LocalDateTime.now(), AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()), null)
        val avviksvurderinger = avviksvurdering.findAll(fødselsnummer, 1.januar)
        assertEquals(2, avviksvurderinger.size)
        assertTrue(avviksvurderinger.contains(avviksvurdering1))
        assertTrue(avviksvurderinger.contains(avviksvurdering2))
    }

    @Test
    fun `henter ikke avviksvurderinger med annet skjæringstidspunkt`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val avviksvurdering1 = opprettEn(UUID.randomUUID(), fødselsnummer, 1.januar, SPINNVILL, LocalDateTime.now(), AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()), null)
        val avviksvurdering2 = opprettEn(UUID.randomUUID(), fødselsnummer, 2.februar, SPINNVILL, LocalDateTime.now(), AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()), null)
        val avviksvurderinger = avviksvurdering.findAll(fødselsnummer, 1.januar)
        assertEquals(1, avviksvurderinger.size)
        assertTrue(avviksvurderinger.contains(avviksvurdering1))
        assertFalse(avviksvurderinger.contains(avviksvurdering2))
    }

    @Test
    fun `henter ikke avviksvurderinger med annet fødselsnummer`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val avviksvurdering1 = opprettEn(UUID.randomUUID(), fødselsnummer, 1.januar, SPINNVILL, LocalDateTime.now(), AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()), null)
        val avviksvurdering2 = opprettEn(UUID.randomUUID(), Fødselsnummer("0101010101"), 1.januar, SPINNVILL, LocalDateTime.now(), AvviksvurderingDto.SammenligningsgrunnlagDto(emptyMap()), null)
        val avviksvurderinger = avviksvurdering.findAll(fødselsnummer, 1.januar)
        assertEquals(1, avviksvurderinger.size)
        assertTrue(avviksvurderinger.contains(avviksvurdering1))
        assertFalse(avviksvurderinger.contains(avviksvurdering2))
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

    private fun opprettEn(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        kilde: AvviksvurderingDto.KildeDto,
        opprettet: LocalDateTime,
        sammenligningsgrunnlag: AvviksvurderingDto.SammenligningsgrunnlagDto,
        beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto?,
    ): AvviksvurderingDto {
        val enAvviksvurdering = AvviksvurderingDto(id, fødselsnummer, skjæringstidspunkt, opprettet, kilde, sammenligningsgrunnlag, beregningsgrunnlag)
        avviksvurdering.upsertAll(listOf(enAvviksvurdering))
        return avviksvurdering.findAll(fødselsnummer, skjæringstidspunkt).last()
    }
}
