package no.nav.helse.mediator.producer

import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.helpers.januar
import no.nav.helse.helpers.objectMapper
import no.nav.helse.helpers.toJson
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import kotlin.test.assertEquals

class BehovProducerTest {

    private val behovProducer = BehovProducer(
        utkastTilVedtakJson = objectMapper.valueToTree(mapOf("etUtkast" to "tilVedtak")),
    )

    @Test
    fun `Kan lage behov for sammenligningsgrunnlag`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )
        val messages = behovProducer.ferdigstill()
        assertEquals(1, messages.size)

        val message = messages[0]
        check(message is Message.Behov)
        val json = message.innhold.toJson()
        assertEquals(setOf("InntekterForSammenligningsgrunnlag"), message.behov)
        assertEquals("2018-01-01", json["InntekterForSammenligningsgrunnlag"].path("skjæringstidspunkt").asText())
        assertEquals("2018-01", json["InntekterForSammenligningsgrunnlag"].path("beregningStart").asText())
        assertEquals("2018-04", json["InntekterForSammenligningsgrunnlag"].path("beregningSlutt").asText())
    }

    @Test
    fun `lager ikke behov når behovskø er tom`() {
        assertEquals(0, behovProducer.ferdigstill().size)
    }

    @Test
    fun `behovkø tømmes etter hver finalize`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )
        val meldinger = behovProducer.ferdigstill()
        val meldingerEtterClear = behovProducer.ferdigstill()
        assertEquals(1, meldinger.size)
        assertEquals(0, meldingerEtterClear.size)
    }

    @Test
    fun `legger med utkast til vedtak på behov`() {
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(1.januar, YearMonth.of(2018, Month.JANUARY), YearMonth.of(2018, Month.APRIL))
        )

        val behov = behovProducer.ferdigstill()[0].innhold.toJson()
        assertEquals("tilVedtak", behov["utkastTilVedtak"]["etUtkast"].asText())
    }
}