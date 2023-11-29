package no.nav.helse.mediator

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals

class BehovProducerTest {

    private val testRapid = TestRapid()
    private val vedtaksperiodeId = UUID.randomUUID()

    private val behovProducer = BehovProducer(
        aktørId = "1234567891011",
        fødselsnummer = "12345678910",
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = "000000000",
        utkastTilVedtakJson = objectMapper.valueToTree(mapOf("etUtkast" to "tilVedtak")),
        rapidsConnection = testRapid
    )

    @Test
    fun `Kan lage behov for sammenligningsgrunnlag`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )
        behovProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)

        val behov = testRapid.inspektør.message(0)
        assertEquals("behov", behov["@event_name"].asText())
        assertEquals("12345678910", behov["fødselsnummer"].asText())
        assertEquals("1234567891011", behov["aktørId"].asText())
        assertEquals("000000000", behov["organisasjonsnummer"].asText())
        assertEquals(vedtaksperiodeId.toString(), behov["vedtaksperiodeId"].asText())
        assertEquals(listOf("InntekterForSammenligningsgrunnlag"), behov["@behov"].map { it.asText() })
        assertEquals("2018-01-01", behov["InntekterForSammenligningsgrunnlag"].path("skjæringstidspunkt").asText())
        assertEquals("2018-01", behov["InntekterForSammenligningsgrunnlag"].path("beregningStart").asText())
        assertEquals("2018-04", behov["InntekterForSammenligningsgrunnlag"].path("beregningSlutt").asText())
    }

    @Test
    fun `lager ikke behov når behovskø er tom`() {
        behovProducer.finalize()
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `behovkø tømmes etter hver finalize`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )
        behovProducer.finalize()
        behovProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `ikke send ut behov før finalize blir kalt`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )
        assertEquals(0, testRapid.inspektør.size)
        behovProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `legger med utkast til vedtak på behov`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )
        behovProducer.finalize()
        val behov = testRapid.inspektør.message(0)
        assertEquals("tilVedtak", behov["utkastTilVedtak"]["etUtkast"].asText())
    }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}