package no.nav.helse.mediator

import no.nav.helse.TestDatabase
import no.nav.helse.db.Dao
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MediatorTest {

    private val testRapid = TestRapid()
    private val dao = Dao(TestDatabase.dataSource())
    init {
        Mediator(testRapid, dao)
    }

    @Disabled
    @Test
    fun `Henter sammenligningsgrunnlag hvis vi ikke har det`() {
        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", "12345678910", "000000000", 1.januar))
        assertEquals(1, testRapid.inspektør.size)
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
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