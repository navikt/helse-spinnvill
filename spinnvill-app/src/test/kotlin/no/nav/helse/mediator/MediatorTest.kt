package no.nav.helse.mediator

import no.nav.helse.db.TestDatabase
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

internal class MediatorTest {

    private val testRapid = TestRapid()
    init {
        Mediator(testRapid, TestDatabase.database())
    }

    @Test
    fun `sender behov for sammenligningsgrunnlag`() {
        val skjæringstidspunkt = 1.januar
        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", "12345678910", "987654321", skjæringstidspunkt))

        val beregningsperiodeTom = YearMonth.from(skjæringstidspunkt.minusMonths(1))
        val beregningsperiodeFom = beregningsperiodeTom.minusMonths(11)

        assertEquals(1, testRapid.inspektør.size)
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(beregningsperiodeFom, testRapid.inspektør.field(0, "InntekterForSammenligningsgrunnlag").path("beregningStart").asYearMonth())
        assertEquals(beregningsperiodeTom, testRapid.inspektør.field(0, "InntekterForSammenligningsgrunnlag").path("beregningSlutt").asYearMonth())
    }

    private fun utkastTilVedtakJson(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate
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
              "aktørId": "$aktørId",
              "fødselsnummer": "$fødselsnummer",
              "organisasjonsnummer": "$organisasjonsnummer",
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "tilstand": "AVVENTER_GODKJENNING",
              "utbetalingId": "db8ff403-6ea3-4ad2-bfdf-d876e28c5839",
              "Godkjenning": {
                "periodeFom": "$skjæringstidspunkt",
                "periodeTom": "${skjæringstidspunkt.plusDays(30)}",
                "skjæringstidspunkt": "$skjæringstidspunkt",
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
                    "organisasjonsnummer": "$organisasjonsnummer",
                    "beløp": 500000.0
                  },
                  {
                    "organisasjonsnummer": "000000000",
                    "beløp": 200000.20
                  }
                ]
              },
              "@id": "ba376523-62b1-49d7-8647-f902c739b634",
              "@opprettet": "2018-01-01T00:00:00.000"
            }
        """.trimIndent()
        return json
    }
}