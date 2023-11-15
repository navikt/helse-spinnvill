package no.nav.helse.mediator

import no.nav.helse.modell.BehovForSammenligningsgrunnlag
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class BehovProducerTest {

    private val testRapid = TestRapid()
    private val vedtaksperiodeId = UUID.randomUUID()

    private val behovProducer = BehovProducer("1234567891011","12345678910", vedtaksperiodeId, "000000000", testRapid)

    @Test
    fun `Kan lage behov for sammenligningsgrunnlag`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
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
        assertEquals("2018-01", behov["InntekterForSammenligningsgrunnlag"].path("beregningStart").asText())
        assertEquals("2018-04", behov["InntekterForSammenligningsgrunnlag"].path("beregningSlutt").asText())
    }
}