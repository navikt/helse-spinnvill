package no.nav.helse.mediator

import no.nav.helse.Fødselsnummer
import no.nav.helse.InntektPerMåned
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.db.TestDatabase
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class MediatorTest {

    private val testRapid = TestRapid()
    private val database = TestDatabase.database()

    init {
        Mediator(testRapid, database)
    }

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
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

    @Test
    fun `sender ikke behov for sammenligningsgrunnlag når det finnes en ekisterende avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val organisasjonsnummer = Organisasjonsnummer("987654321")

        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(organisasjonsnummer to listOf(AvviksvurderingDto.MånedligInntektDto(InntektPerMåned(20000.0), YearMonth.from(skjæringstidspunkt))))
            ),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
                mapOf(organisasjonsnummer to OmregnetÅrsinntekt(400000.0))
            )
        )

        database.lagreAvviksvurdering(avviksvurderingDto)

        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", fødselsnummer.value, organisasjonsnummer.value, skjæringstidspunkt))

        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `motta sammenligningsgrunnlag`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val organisasjonsnummer = Organisasjonsnummer("987654321")
        val skjæringstidspunkt = 1.januar

        testRapid.sendTestMessage(sammenligningsgrunnlagJson("1234567891011", fødselsnummer.value, organisasjonsnummer.value, skjæringstidspunkt))

        assertNotNull(database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt))
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
}