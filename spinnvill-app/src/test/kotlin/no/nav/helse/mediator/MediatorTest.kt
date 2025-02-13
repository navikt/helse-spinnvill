package no.nav.helse.mediator

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.db.Database
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class MediatorTest {
    private val fødselsnummer = "12345678910".somFnr()
    private val organisasjonsnummer = "987654321".somArbeidsgiverref()
    private val skjæringstidspunkt = 1.januar
    private val beregningsgrunnlag = Beregningsgrunnlag.opprett(
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
        val testRapid = TestRapid()
        val mediator = Mediator(VersjonAvKode("versjon"), testRapid) { database }
        mediator.håndter(SammenligningsgrunnlagLøsning(fødselsnummer, skjæringstidspunkt, UUID.randomUUID(), sammenligningsgrunnlag))
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `ignorerer sammenligningsgrunnlag-løsning dersom ubehandlet behov ikke har samme id som avviksvurderingbehov-iden på løsningen`() {
        val enBehovId = UUID.randomUUID()
        val enAnnenBehovId = UUID.randomUUID()
        val etUbehandletAvviksvurderingBehov = avviksvurderingBehov(enBehovId)
        val database = databaseStub(etUbehandletAvviksvurderingBehov)
        val testRapid = TestRapid()
        val mediator = Mediator(VersjonAvKode("versjon"), testRapid) { database }
        mediator.håndter(SammenligningsgrunnlagLøsning(fødselsnummer, skjæringstidspunkt, enAnnenBehovId, sammenligningsgrunnlag))
        assertEquals(0, testRapid.inspektør.size)
    }

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

    private fun databaseStub(returnedAvviksvurderingBehov: AvviksvurderingBehov? = null) = object : Database {
        override fun datasource(): HikariDataSource = error("Not implemented in test")
        override fun migrate() = error("Not implemented in test")
        override fun finnSisteAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): AvviksvurderingDto = error("Not implemented in test")
        override fun lagreAvviksvurderingBehov(avviksvurderingBehov: AvviksvurderingBehov) = error("Not implemented in test")
        override fun finnAvviksvurderingsgrunnlag(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): List<AvviksvurderingDto> = error("Not implemented in test")
        override fun lagreGrunnlagshistorikk(avviksvurderinger: List<AvviksvurderingDto>) = error("Not implemented in test")

        override fun finnUbehandledeAvviksvurderingBehov(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
        ): AvviksvurderingBehov? = returnedAvviksvurderingBehov
    }
}
