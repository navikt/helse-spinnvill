package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.*
import no.nav.helse.db.TestDatabase
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

internal class MediatorTest {

    private val testRapid = TestRapid()
    private val database = TestDatabase.database()

    init {
        Mediator(VersjonAvKode("1.0.0"), testRapid, database)
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
        assertEquals(skjæringstidspunkt, testRapid.inspektør.field(0, "InntekterForSammenligningsgrunnlag").path("skjæringstidspunkt").asLocalDate())
    }

    @Test
    fun `sender ikke behov for sammenligningsgrunnlag når det finnes en ekisterende avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val arbeidsgiverreferanse = Arbeidsgiverreferanse("987654321")

        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(arbeidsgiverreferanse to listOf(AvviksvurderingDto.MånedligInntektDto(
                    inntekt = InntektPerMåned(value = 20000.0),
                    måned = YearMonth.from(skjæringstidspunkt),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                )))
            ),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
                mapOf(arbeidsgiverreferanse to OmregnetÅrsinntekt(400000.0))
            )
        )

        database.lagreAvviksvurdering(avviksvurderingDto)

        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", fødselsnummer.value, arbeidsgiverreferanse.value, skjæringstidspunkt))

        assertEquals(0, testRapid.inspektør.behov("InntekterForSammenligningsgrunnlag").size)
    }

    @Test
    fun `motta sammenligningsgrunnlag`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val arbeidsgiverreferanse = Arbeidsgiverreferanse("987654321")
        val skjæringstidspunkt = 1.januar

        testRapid.sendTestMessage(sammenligningsgrunnlagJson("1234567891011", fødselsnummer.value, arbeidsgiverreferanse.value, skjæringstidspunkt))

        assertNotNull(database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt))
    }

    @Test
    fun `gjør en vellykket avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val arbeidsgiverreferanse = Arbeidsgiverreferanse("987654321")

        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(arbeidsgiverreferanse to listOf(AvviksvurderingDto.MånedligInntektDto(
                    inntekt = InntektPerMåned(value = 20000.0),
                    måned = YearMonth.from(skjæringstidspunkt),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                )))
            ),
            beregningsgrunnlag = null
        )

        database.lagreAvviksvurdering(avviksvurderingDto)

        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", fødselsnummer.value, arbeidsgiverreferanse.value, skjæringstidspunkt))

        val fullstendigAvviksvurdering = database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)

        assertNotNull(fullstendigAvviksvurdering)
        assertNotNull(fullstendigAvviksvurdering.beregningsgrunnlag)

        assertEquals(2, testRapid.inspektør.size)
        assertEquals(1, testRapid.inspektør.hendelser("nye_varsler").size)
        assertEquals(1, testRapid.inspektør.hendelser("subsumsjon").size)
    }

    @Test
    fun `gjør ikke ny avviksvurdering om beregningsgrunnlaget er likt som for forrige avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val arbeidsgiverreferanse = Arbeidsgiverreferanse("987654321")

        val beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(arbeidsgiverreferanse to OmregnetÅrsinntekt(300000.0))
        )
        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(arbeidsgiverreferanse to listOf(AvviksvurderingDto.MånedligInntektDto(
                    inntekt = InntektPerMåned(value = 20000.0),
                    måned = YearMonth.from(skjæringstidspunkt),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                )))
            ),
            beregningsgrunnlag = beregningsgrunnlag
        )

        val avviksvurdering = database.lagreAvviksvurdering(avviksvurderingDto)

        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", fødselsnummer.value, arbeidsgiverreferanse.value, skjæringstidspunkt, beregningsgrunnlag))

        val sisteAvviksvurdering = database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)

        assertEquals(avviksvurdering, sisteAvviksvurdering)
    }

    @Test
    fun `gjør ny avviksvurdering om beregningsgrunnlaget er forskjellig fra forrige avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val arbeidsgiverreferanse = Arbeidsgiverreferanse("987654321")

        val avviksvurderingDto = AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = AvviksvurderingDto.SammenligningsgrunnlagDto(
                mapOf(arbeidsgiverreferanse to listOf(AvviksvurderingDto.MånedligInntektDto(
                    inntekt = InntektPerMåned(value = 20000.0),
                    måned = YearMonth.from(skjæringstidspunkt),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                )))
            ),
            beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
                mapOf(arbeidsgiverreferanse to OmregnetÅrsinntekt(300000.0))
            )
        )

        val avviksvurdering = database.lagreAvviksvurdering(avviksvurderingDto)

        testRapid.sendTestMessage(utkastTilVedtakJson("1234567891011", fødselsnummer.value, arbeidsgiverreferanse.value, skjæringstidspunkt))

        val sisteAvviksvurdering = database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)

        assertNotNull(sisteAvviksvurdering)
        assertNotEquals(avviksvurdering.id, sisteAvviksvurdering.id)
        assertNotEquals(avviksvurdering.beregningsgrunnlag, sisteAvviksvurdering.beregningsgrunnlag)
        assertEquals(avviksvurdering.fødselsnummer, sisteAvviksvurdering.fødselsnummer)
        assertEquals(avviksvurdering.skjæringstidspunkt, sisteAvviksvurdering.skjæringstidspunkt)
        assertEquals(avviksvurdering.sammenligningsgrunnlag, sisteAvviksvurdering.sammenligningsgrunnlag)
    }

    @Test
    fun `håndter utkast til vedtak på ny etter å ha mottatt sammenligningsgrunnlag`() {
        val skjæringstidspunkt = 1.januar
        val fødselsnummer = "12345678910".somFnr()
        val organisasjonsnummer = "987654321".somArbeidsgiverref()
        val beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(
                organisasjonsnummer to OmregnetÅrsinntekt(500000.0),
            )
        )

        testRapid.sendTestMessage(utkastTilVedtakJson(
            aktørId = "1234567891011",
            fødselsnummer = fødselsnummer.value,
            organisasjonsnummer = organisasjonsnummer.value,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlagDto = beregningsgrunnlag
        ))

        håndterSammenligningsgrunnlagMelding()

        val avviksvurdering = database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)

        assertNotNull(avviksvurdering)
        assertEquals(fødselsnummer, avviksvurdering.fødselsnummer)
        assertEquals(skjæringstidspunkt, avviksvurdering.skjæringstidspunkt)
        assertEquals(
            mapOf(
                organisasjonsnummer to listOf(
                    AvviksvurderingDto.MånedligInntektDto(
                        inntekt = InntektPerMåned(10000.0),
                        måned = YearMonth.from(1.januar),
                        fordel = null,
                        beskrivelse = null,
                        inntektstype = AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                    )
                )
            ), avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter
        )
        assertEquals(beregningsgrunnlag, avviksvurdering.beregningsgrunnlag)
    }

    @Test
    fun `bruker ikke nytt sammenligningsgrunnlag hvis vi har en eksisterende vilkårsvurdering for samme fnr og skjæringstidspunkt`() {
        val skjæringstidspunkt = 1.januar
        val fødselsnummer = "12345678910".somFnr()
        val organisasjonsnummer = "987654321".somArbeidsgiverref()
        val beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(
                organisasjonsnummer to OmregnetÅrsinntekt(500000.0),
            )
        )

        testRapid.sendTestMessage(utkastTilVedtakJson(
            aktørId = "1234567891011",
            fødselsnummer = fødselsnummer.value,
            organisasjonsnummer = organisasjonsnummer.value,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlagDto = beregningsgrunnlag
        ))

        håndterSammenligningsgrunnlagMelding()
        val avviksvurdering1 = database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)

        håndterSammenligningsgrunnlagMelding()
        val avviksvurdering2 = database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)

        assertEquals(avviksvurdering1, avviksvurdering2)
    }

    private fun håndterSammenligningsgrunnlagMelding() {
        val melding = testRapid.inspektør.sisteBehov("InntekterForSammenligningsgrunnlag")?.deepCopy<ObjectNode>()

        assertNotNull(melding)

        val løsning =
            mapOf(
                "InntekterForSammenligningsgrunnlag" to listOf(
                    mapOf(
                        "årMåned" to YearMonth.from(1.januar),
                        "inntektsliste" to listOf(
                            mapOf(
                                "beløp" to 10000.0,
                                "inntektstype" to "LOENNSINNTEKT",
                                "orgnummer" to "987654321"
                            )
                        )
                    )
                )
            )

        melding.replace("@løsning", objectMapper.valueToTree(løsning))
        melding.replace("@final", objectMapper.valueToTree(true))

        testRapid.sendTestMessage(objectMapper.writeValueAsString(melding))
    }

    private fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    private fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.sisteBehov(vararg behov: String) =
        hendelser("behov")
            .last()
            .takeIf { it.path("@behov").map(JsonNode::asText).containsAll(behov.toList()) && !it.hasNonNull("@løsning") }

    private fun TestRapid.RapidInspector.behov(behov: String) =
        hendelser("behov")
            .filter { it.path("@behov").map(JsonNode::asText).containsAll(listOf(behov)) }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private fun utkastTilVedtakJson(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        beregningsgrunnlagDto: AvviksvurderingDto.BeregningsgrunnlagDto = AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(
                Arbeidsgiverreferanse(organisasjonsnummer) to OmregnetÅrsinntekt(500000.0),
                Arbeidsgiverreferanse("000000000") to OmregnetÅrsinntekt(200000.20)
            )
        )
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
                "omregnedeÅrsinntekter": ${beregningsgrunnlagDto.toJson()} 
              },
              "@id": "ba376523-62b1-49d7-8647-f902c739b634",
              "@opprettet": "2018-01-01T00:00:00.000"
            }
        """.trimIndent()
        return json
    }

    private fun AvviksvurderingDto.BeregningsgrunnlagDto.toJson(): String {
        return objectMapper.writeValueAsString(this.omregnedeÅrsinntekter.map { mapOf("organisasjonsnummer" to it.key, "beløp" to it.value) })
    }

    private fun sammenligningsgrunnlagJson(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        final: Boolean = true
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
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "InntekterForSammenligningsgrunnlag": {
                "skjæringstidspunkt": "$skjæringstidspunkt",
                "beregningStart": "2018-01",
                "beregningSlutt": "2018-02"
              },
              "utkastTilVedtak": {},
              "@id": "ecfe47f6-2063-451a-b7e1-182490cc3153",
              "@opprettet": "2018-01-01T00:00:00.000",
              "@final": $final,
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