package no.nav.helse.kafka

import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SammenligningsgrunnlagRiverTest {

    private val testRapid = TestRapid()

    private val messageHandler = object: MessageHandler {
        val messages = mutableListOf<SammenligningsgrunnlagMessage>()

        override fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage) {}

        override fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage) {
            messages.add(sammenligningsgrunnlagMessage)
        }
    }

    private companion object {
        private const val AKTØRID = "1234567891011"
        private const val FØDSELSNUMMER = "12345678910"
        private const val ORGANISASJONSNUMMER = "987654321"
    }

    init {
        SammenligningsgrunnlagRiver(testRapid, messageHandler)
    }

    @Test
    fun `Leser inn sammenligningsgrunnlag løsning`() {
        testRapid.sendTestMessage(sammenligningsgrunnlagJson(AKTØRID, FØDSELSNUMMER, ORGANISASJONSNUMMER, 1.januar))
        assertEquals(1, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag uten løsning`() {
        testRapid.sendTestMessage(sammenligningsgrunnlagJsonUtenLøsning(AKTØRID, FØDSELSNUMMER, ORGANISASJONSNUMMER, 1.januar))
        assertEquals(0, messageHandler.messages.size)
    }

    private fun sammenligningsgrunnlagJson(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate
    ): String {
        @Language("JSON")
        val json = """
            {
              "@event_name": "behov",
              "@behovId": "ed8f2e02-15b1-45a7-88e4-3b2f0b9cda73",
              "@behov": [
                "InntekterForSammenligningsgrunnlag"
              ],
              "meldingsreferanseId": "ff032457-203f-43ec-8850-b72a57ad9e52",
              "aktørId": "$aktørId",
              "fødselsnummer": "$fødselsnummer",
              "organisasjonsnummer": "$organisasjonsnummer",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "InntekterForSammenligningsgrunnlag": {
                "beregningStart": "2018-01",
                "beregningSlutt": "2018-02"
              },
              "@id": "ecfe47f6-2063-451a-b7e1-182490cc3153",
              "@opprettet": "2018-01-01T00:00:00.000",
              "@løsning": {
                "InntekterForSammenligningsgrunnlag": [
                  {
                    "årMåned": "2018-01",
                    "arbeidsforholdliste": [],
                    "inntektsliste": [
                      {
                        "beløp": 20000.00,
                        "inntektstype": "LOENNSINNTEKT",
                        "orgnummer": "$organisasjonsnummer",
                        "fødselsnummer": null,
                        "aktørId": null,
                        "beskrivelse": "skattepliktigDelForsikringer",
                        "fordel": "naturalytelse"
                      },
                      {
                        "beløp": 50000.00,
                        "inntektstype": "LOENNSINNTEKT",
                        "orgnummer": "000000000",
                        "fødselsnummer": null,
                        "aktørId": null,
                        "beskrivelse": "fastloenn",
                        "fordel": "kontantytelse"
                      }
                    ]
                  },
                  {
                    "årMåned": "2018-02",
                    "arbeidsforholdliste": [],
                    "inntektsliste": [
                      {
                        "beløp": 20000.00,
                        "inntektstype": "LOENNSINNTEKT",
                        "orgnummer": "$organisasjonsnummer",
                        "fødselsnummer": null,
                        "aktørId": null,
                        "beskrivelse": "skattepliktigDelForsikringer",
                        "fordel": "naturalytelse"
                      },
                      {
                        "beløp": 50000.00,
                        "inntektstype": "LOENNSINNTEKT",
                        "orgnummer": "000000000",
                        "fødselsnummer": null,
                        "aktørId": null,
                        "beskrivelse": "fastloenn",
                        "fordel": "kontantytelse"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        return json
    }
    private fun sammenligningsgrunnlagJsonUtenLøsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate
    ): String {
        @Language("JSON")
        val json = """
            {
              "@event_name": "behov",
              "@behovId": "ed8f2e02-15b1-45a7-88e4-3b2f0b9cda73",
              "@behov": [
                "InntekterForSammenligningsgrunnlag"
              ],
              "meldingsreferanseId": "ff032457-203f-43ec-8850-b72a57ad9e52",
              "aktørId": "$aktørId",
              "fødselsnummer": "$fødselsnummer",
              "organisasjonsnummer": "$organisasjonsnummer",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "InntekterForSammenligningsgrunnlag": {
                "beregningStart": "2018-01",
                "beregningSlutt": "2018-02"
              },
              "@id": "ecfe47f6-2063-451a-b7e1-182490cc3153",
              "@opprettet": "2018-01-01T00:00:00.000"
            }
        """.trimIndent()
        return json
    }
}