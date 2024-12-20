package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastsattIInfotrygdRiverTest {

    private val testRapid = TestRapid()

    init {
        FastsattIInfotrygdRiver(testRapid)
    }

    @Test
    fun `les inn IInfotrygd`() {
        testRapid.sendTestMessage(utkastTilVedtakJson())
        val message = testRapid.inspektør.message(0)
        assertEquals(1, testRapid.inspektør.size)
        assertEquals("behov", message["@event_name"].asText())
        assertEquals(listOf("Godkjenning"), message["@behov"].map { it.asText() } )
        assertFalse(message["behandletAvSpinnvill"].isMissingOrNull())
        assertTrue(message["behandletAvSpinnvill"].asBoolean())
    }

    @Test
    fun `ikke les inn EtterHovedregel eller EtterSkjønn`() {
        testRapid.sendTestMessage(utkastTilVedtakJson("EtterHovedregel"))
        testRapid.sendTestMessage(utkastTilVedtakJson("EtterSkjønn"))
        assertEquals(0, testRapid.inspektør.size)
    }

    private fun utkastTilVedtakJson(
        fastsatt: String = "IInfotrygd",
    ): String {
        @Language("JSON")
        val json = """
            {
              "@event_name": "behov",
              "@behovId": "c64a73be-7337-4f25-8923-94f355c23d76",
              "@behov": [
                "Godkjenning"
              ],
              "meldingsreferanseId": "b63537e5-ffd9-4e9b-930c-45b0ab602d66",
              "fødselsnummer": "12345678910",
              "organisasjonsnummer": "00000000",
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "tilstand": "AVVENTER_GODKJENNING",
              "utbetalingId": "db8ff403-6ea3-4ad2-bfdf-d876e28c5839",
              "Godkjenning": {
                "periodeFom": "2018-01-01",
                "periodeTom": "2018-01-31",
                "skjæringstidspunkt": "2018-01-01",
                "vilkårsgrunnlagId": "87b9339d-a67d-49b0-af36-c93d6f9249ae",
                "periodetype": "FØRSTEGANGSBEHANDLING",
                "førstegangsbehandling": true,
                "utbetalingtype": "UTBETALING",
                "inntektskilde": "EN_ARBEIDSGIVER",
                "orgnummereMedRelevanteArbeidsforhold": [],
                "tags": [
                  "EN_ARBEIDSGIVER",
                  "ARBEIDSGIVERUTBETALING"
                ],
                "kanAvvises": true,
                "omregnedeÅrsinntekter": [
                  {
                    "organisasjonsnummer": "000000000",
                    "beløp": 500000.0
                  }
                ],
                "sykepengegrunnlagsfakta": {
                  "omregnetÅrsinntektTotalt": 50000.0,
                  "sykepengegrunnlag": 50000.0,
                  "fastsatt": "$fastsatt"
                }
              },
              "@id": "ba376523-62b1-49d7-8647-f902c739b634",
              "@opprettet": "2018-01-01T00:00:00.000"
            }
        """.trimIndent()
        return json
    }
}