package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.db.Database
import no.nav.helse.helpers.dummyBeregningsgrunnlag
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class MediatorTest {
    private val fødselsnummer = "12345678910".somFnr()
    private val organisasjonsnummer = "987654321".somArbeidsgiverref()
    private val skjæringstidspunkt = 1.januar
    private val beregningsgrunnlag = Beregningsgrunnlag(
        mapOf(
            organisasjonsnummer to OmregnetÅrsinntekt(600000.0),
        )
    )

    private val sammenligningsgrunnlag = Sammenligningsgrunnlag(
        inntekter = listOf(
            ArbeidsgiverInntekt(
                organisasjonsnummer, inntekter = listOf(
                    MånedligInntekt(
                        InntektPerMåned(50000.0),
                        måned = januar(),
                        fordel = null,
                        beskrivelse = null,
                        inntektstype = LØNNSINNTEKT
                    )
                )
            )
        )
    )

    @Test
    fun `ignorerer sammenligningsgrunnlag-løsning dersom det ikke finnes noe ubehandlet avviksvurdering-behov`() {
        val database = databaseStub()
        database.lagreGrunnlagshistorikk(testAvviksvurderingsGrunnlag())
        val testRapid = TestRapid()
        val mediator = Mediator(VersjonAvKode("versjon"), testRapid) { database }
        mediator.håndter(
            SammenligningsgrunnlagLøsning(
                fødselsnummer,
                skjæringstidspunkt,
                UUID.randomUUID(),
                sammenligningsgrunnlag
            )
        )
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `Ignorerer avviksvurderingsbehov som ligger ubesvart i databasen`() {
        val database = databaseStub()
        val testRapid = TestRapid()
        val mediator = Mediator(VersjonAvKode("versjon"), testRapid) { database }

        val behovId1 = UUID.randomUUID()
        mediator.håndter(
            AvviksvurderingBehov.nyttBehov(
                vilkårsgrunnlagId = UUID.randomUUID(),
                behovId = behovId1,
                skjæringstidspunkt = skjæringstidspunkt,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = organisasjonsnummer,
                beregningsgrunnlag = dummyBeregningsgrunnlag,
                json = emptyMap()
            )
        )
        assertTrue(database.avviksvurderingBehovErLagret(behovId1))
        assertEquals(1, testRapid.inspektør.size)
        assertEquals(
            listOf("InntekterForSammenligningsgrunnlag"),
            testRapid.inspektør.message(0)["@behov"].map { it.asText() })

        val behovId2 = UUID.randomUUID()
        mediator.håndter(
            AvviksvurderingBehov.nyttBehov(
                vilkårsgrunnlagId = UUID.randomUUID(),
                behovId = behovId2,
                skjæringstidspunkt = skjæringstidspunkt,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = organisasjonsnummer,
                beregningsgrunnlag = dummyBeregningsgrunnlag,
                json = emptyMap()
            )
        )
        assertFalse(database.avviksvurderingBehovErLagret(behovId2))
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `Sletter avviksvurderingsbehov som ligger ubesvart i databasene i over en time, og fortsetter håndtering`() {
        val database = databaseStub()
        val testRapid = TestRapid()
        val mediator = Mediator(VersjonAvKode("versjon"), testRapid) { database }

        val behovId1 = UUID.randomUUID()
        mediator.håndter(
            AvviksvurderingBehov.fraLagring(
                vilkårsgrunnlagId = UUID.randomUUID(),
                behovId = behovId1,
                skjæringstidspunkt = skjæringstidspunkt,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = organisasjonsnummer,
                beregningsgrunnlag = dummyBeregningsgrunnlag,
                json = emptyMap(),
                opprettet = LocalDateTime.now().minusMinutes(61),
                løst = false
            )
        )
        assertTrue(database.avviksvurderingBehovErLagret(behovId1))
        assertEquals(1, testRapid.inspektør.size)
        assertEquals(
            listOf("InntekterForSammenligningsgrunnlag"),
            testRapid.inspektør.message(0)["@behov"].map { it.asText() })

        val behovId2 = UUID.randomUUID()
        mediator.håndter(
            AvviksvurderingBehov.nyttBehov(
                vilkårsgrunnlagId = UUID.randomUUID(),
                behovId = behovId2,
                skjæringstidspunkt = skjæringstidspunkt,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = organisasjonsnummer,
                beregningsgrunnlag = dummyBeregningsgrunnlag,
                json = emptyMap()
            )
        )
        assertTrue(database.avviksvurderingBehovErLagret(behovId2))
        assertFalse(database.avviksvurderingBehovErLagret(behovId1))
        assertEquals(2, testRapid.inspektør.size)
        assertEquals(
            listOf("InntekterForSammenligningsgrunnlag"),
            testRapid.inspektør.message(1)["@behov"].map { it.asText() })
    }

    @Test
    fun `ignorerer sammenligningsgrunnlag-løsning dersom ubehandlet behov ikke har samme id som avviksvurderingbehov-iden på løsningen`() {
        val enBehovId = UUID.randomUUID()
        val enAnnenBehovId = UUID.randomUUID()
        val etUbehandletAvviksvurderingBehov = avviksvurderingBehov(enBehovId)
        val database = databaseStub()
        database.lagreAvviksvurderingBehov(etUbehandletAvviksvurderingBehov)
        database.lagreGrunnlagshistorikk(testAvviksvurderingsGrunnlag())
        val testRapid = TestRapid()
        val mediator = Mediator(VersjonAvKode("versjon"), testRapid) { database }
        mediator.håndter(
            SammenligningsgrunnlagLøsning(
                fødselsnummer,
                skjæringstidspunkt,
                enAnnenBehovId,
                sammenligningsgrunnlag
            )
        )
        assertEquals(0, testRapid.inspektør.size)
    }

    private fun testAvviksvurderingsGrunnlag() = listOf(
        Avviksvurderingsgrunnlag(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = Beregningsgrunnlag(
                mapOf(
                    organisasjonsnummer to OmregnetÅrsinntekt(
                        600000.0
                    )
                )
            ),
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            opprettet = LocalDateTime.now(),
            kilde = Kilde.INFOTRYGD,
        )
    )

    private fun avviksvurderingBehov(behovId: UUID): AvviksvurderingBehov {
        return AvviksvurderingBehov.nyttBehov(
            vilkårsgrunnlagId = UUID.randomUUID(),
            behovId = behovId,
            skjæringstidspunkt = skjæringstidspunkt,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = UUID.randomUUID(),
            organisasjonsnummer = organisasjonsnummer,
            beregningsgrunnlag = beregningsgrunnlag,
            json = emptyMap()
        )
    }


    private fun databaseStub() = object : Database {
        private val avviksvurderingBehovMap = mutableMapOf<UUID, AvviksvurderingBehov>()
        private val avviksvurderingGrunnlagMap = mutableMapOf<UUID, Avviksvurderingsgrunnlag>()
        override fun datasource(): HikariDataSource = error("Not implemented in test")
        override fun migrate() = error("Not implemented in test")
        override fun lagreAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov) {
            avviksvurderingBehovMap[avviksvurderingBehov.behovId] = avviksvurderingBehov
        }

        override fun finnAvviksvurderingsgrunnlag(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
        ): List<Avviksvurderingsgrunnlag> =
            avviksvurderingGrunnlagMap.values.filter { it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt }

        override fun lagreGrunnlagshistorikk(grunnlagene: List<Avviksvurderingsgrunnlag>) {
            grunnlagene.forEach { grunnlag -> avviksvurderingGrunnlagMap[grunnlag.id] = grunnlag }
        }

        override fun slettAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov) {
            avviksvurderingBehovMap.remove(avviksvurderingBehov.behovId)
        }

        override fun finnUbehandletAvviksvurderingBehov(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
        ): AvviksvurderingBehov? =
            avviksvurderingBehovMap.values.find { it.fødselsnummer == fødselsnummer && it.skjæringstidspunkt == skjæringstidspunkt && !it.erLøst() }

        fun avviksvurderingBehovErLagret(behovId: UUID) = avviksvurderingBehovMap[behovId] != null
    }
}
