package no.nav.helse.db

import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.helpers.februar
import no.nav.helse.helpers.januar
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class AvviksvurderingTest {

    private val database = TestDatabase.database()
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
        val beregningsgrunnlag = beregningsgrunnlag()

        val id = UUID.randomUUID()
        val avviksvurdering = opprettEn(
            id,
            fødselsnummer,
            skjæringstidspunkt,
            LocalDateTime.now(),
            sammenligningsgrunnlag,
            beregningsgrunnlag
        )
        assertNotNull(avviksvurdering)

        assertEquals(id, avviksvurdering.id)
        assertEquals(fødselsnummer, avviksvurdering.fødselsnummer)
        assertEquals(skjæringstidspunkt, avviksvurdering.skjæringstidspunkt)
        assertEquals(sammenligningsgrunnlag.inntekter, avviksvurdering.sammenligningsgrunnlag.inntekter)
        assertEquals(beregningsgrunnlag, avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `lagrer ikke duplikate avviksvurderingsgrunnlag`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag1 = beregningsgrunnlag(200000.001)
        val beregningsgrunnlag2 = beregningsgrunnlag(300000.05)
        val sammenligningsgrunnlag1 = sammenligningsgrunnlag(20000.0)
        val sammenligningsgrunnlag2 = sammenligningsgrunnlag(30000.0)

        val avviksvurderingsgrunnlag1 = Avviksvurderingsgrunnlag(avviksvurderingId, fødselsnummer, skjæringstidspunkt, beregningsgrunnlag1, sammenligningsgrunnlag1, LocalDateTime.now().minusDays(1))
        avviksvurdering.insertOne(avviksvurderingsgrunnlag1)
        val avviksvurderingsgrunnlag2 = Avviksvurderingsgrunnlag(avviksvurderingId, fødselsnummer, skjæringstidspunkt, beregningsgrunnlag2, sammenligningsgrunnlag2, LocalDateTime.now())
        avviksvurdering.insertOne(avviksvurderingsgrunnlag2)

        val funnetAvviksvurderingsgrunnlag = database.finnAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt)

        assertLike(avviksvurderingsgrunnlag1, funnetAvviksvurderingsgrunnlag)
    }

    @Test
    fun `oppretter ikke nytt sammenligningsgrunnlag om det samme finnes fra før`() {
        val avviksvurderingId = UUID.randomUUID()
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val beregningsgrunnlag = beregningsgrunnlag(200000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag(20000.0)

        opprettEn(
            avviksvurderingId,
            fødselsnummer,
            skjæringstidspunkt,
            LocalDateTime.now().minusDays(1),
            sammenligningsgrunnlag,
            beregningsgrunnlag
        )
        opprettEn(
            avviksvurderingId,
            fødselsnummer,
            skjæringstidspunkt,
            LocalDateTime.now(),
            sammenligningsgrunnlag,
            beregningsgrunnlag
        )

        val antallSammenligningsgrunnlag = transaction {
            Avviksvurdering.Companion.EttSammenligningsgrunnlag.find { Avviksvurdering.Companion.Sammenligningsgrunnlag.avviksvurdering eq avviksvurderingId}.count()
        }

        assertEquals(1, antallSammenligningsgrunnlag)
    }

    @Test
    fun `henter ikke grunnlag med annet skjæringstidspunkt`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val grunnlag1 = opprettEn(
            UUID.randomUUID(),
            fødselsnummer,
            1.januar,
            LocalDateTime.now(),
            sammenligningsgrunnlag(),
            beregningsgrunnlag()
        )
        opprettEn(
            UUID.randomUUID(),
            fødselsnummer,
            2.februar,
            LocalDateTime.now(),
            sammenligningsgrunnlag(),
            beregningsgrunnlag()
        )
        val funnetGrunnlag = avviksvurdering.findLatest(fødselsnummer, 1.januar)
        assertLike(grunnlag1, funnetGrunnlag)
    }

    @Test
    fun `henter ikke avviksvurderinger med annet fødselsnummer`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val grunnlag1 = opprettEn(
            UUID.randomUUID(),
            fødselsnummer,
            1.januar,
            LocalDateTime.now(),
            sammenligningsgrunnlag(),
            beregningsgrunnlag()
        )
        opprettEn(
            UUID.randomUUID(),
            Fødselsnummer("0101010101"),
            1.januar,
            LocalDateTime.now(),
            sammenligningsgrunnlag(),
            beregningsgrunnlag()
        )
        val funnetGrunnlag = avviksvurdering.findLatest(fødselsnummer, 1.januar)
        assertLike(grunnlag1, funnetGrunnlag)
    }

    @Test
    fun `hvis nyeste avviksvurdering ble gjort i Infotrygd er all historikk irrelevant, for da skal det gjøres ny avviksvurdering uansett`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        opprettEn(
            UUID.randomUUID(),
            fødselsnummer,
            1.januar,
            LocalDateTime.now(),
            sammenligningsgrunnlag(),
            beregningsgrunnlag()
        )
        fakeInfotrygdAvviksvurderingsgrunnlag(fødselsnummer, skjæringstidspunkt = 1.januar)
        val funnetGrunnlag = avviksvurdering.findLatest(fødselsnummer, 1.januar)
        assertNull(funnetGrunnlag)
    }

    private fun beregningsgrunnlag(omregnetÅrsinntekt: Double = 20000.0): Beregningsgrunnlag {
        return Beregningsgrunnlag(
            mapOf(Arbeidsgiverreferanse("123456789") to OmregnetÅrsinntekt(omregnetÅrsinntekt))
        )
    }

    private fun sammenligningsgrunnlag(inntektPerMåned: Double = 200000.0): Sammenligningsgrunnlag {
        return Sammenligningsgrunnlag(
            inntekter = listOf(
                ArbeidsgiverInntekt(
                    Arbeidsgiverreferanse("123456789"),
                    inntekter = listOf(
                        ArbeidsgiverInntekt.MånedligInntekt(
                            InntektPerMåned(inntektPerMåned),
                            YearMonth.of(2020, 1),
                            fordel = Fordel("En fordel"),
                            beskrivelse = Beskrivelse("En beskrivelse"),
                            inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                        )

                    )
                )
            )
        )
    }

    private fun assertLike(expected: Avviksvurderingsgrunnlag?, actual: Avviksvurderingsgrunnlag?) {
        assertEquals(expected?.id, actual?.id)
        assertEquals(expected?.fødselsnummer, actual?.fødselsnummer)
        assertEquals(expected?.beregningsgrunnlag, actual?.beregningsgrunnlag)
        assertEquals(expected?.sammenligningsgrunnlag, actual?.sammenligningsgrunnlag)
        assertEquals(expected?.skjæringstidspunkt, actual?.skjæringstidspunkt)
        assertEquals(expected?.opprettet?.withNano(0), actual?.opprettet?.withNano(0))
    }

    private fun opprettEn(
        id: UUID,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        beregningsgrunnlag: Beregningsgrunnlag,
    ): Avviksvurderingsgrunnlag? {
        val etAvviksvurderingsgrunnlag = Avviksvurderingsgrunnlag(id, fødselsnummer, skjæringstidspunkt, beregningsgrunnlag, sammenligningsgrunnlag, opprettet)
        avviksvurdering.insertOne(etAvviksvurderingsgrunnlag)
        return avviksvurdering.findLatest(fødselsnummer, skjæringstidspunkt)
    }

    private fun fakeInfotrygdAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate) {
        val conn = database.datasource().connection
        val stmt = conn.prepareStatement("""
            INSERT INTO avviksvurdering(id, fødselsnummer, skjæringstidspunkt, opprettet, kilde) 
            VALUES (?::uuid, ?, ?::timestamp, ?::timestamp, ?)
            """
        )
        stmt.setString(1, java.util.UUID.randomUUID().toString())
        stmt.setString(2, fødselsnummer.value)
        stmt.setString(3, skjæringstidspunkt.toString())
        stmt.setString(4, java.time.LocalDateTime.now().toString())
        stmt.setString(5, "INFOTRYGD")
        stmt.executeUpdate()
        conn.close()
    }

}
