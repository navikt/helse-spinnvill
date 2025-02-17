package no.nav.helse.kafka

import no.nav.helse.Beskrivelse
import no.nav.helse.Fordel
import no.nav.helse.InntektPerMåned
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.avviksvurdering.SammenligningsgrunnlagLøsning
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.somArbeidsgiverref
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals

class SammenligningsgrunnlagRiverTest {

    private val testRapid = TestRapid()

    private val messageHandler = object : MessageHandler {
        val messages = mutableListOf<SammenligningsgrunnlagLøsning>()

        override fun håndter(message: GodkjenningsbehovMessage) {}
        override fun håndter(behov: AvviksvurderingBehov) {}

        override fun håndter(sammenligningsgrunnlagMessageOld: SammenligningsgrunnlagMessageOld) {}

        override fun håndter(løsning: SammenligningsgrunnlagLøsning) {
            messages.add(løsning)
        }
    }

    private companion object {
        private const val FØDSELSNUMMER = "12345678910"
        private const val ORGANISASJONSNUMMER = "987654321"
    }

    init {
        SammenligningsgrunnlagRiver(testRapid, messageHandler)
    }

    @Test
    fun `Leser inn sammenligningsgrunnlag løsning`() {
        val avviksvurderingBehovId = UUID.randomUUID()
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = avviksvurderingBehovId,
                inntekter = inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt())
                )
            )
        )
        val meldinger = messageHandler.messages
        val melding = meldinger.single()

        assertEquals(FØDSELSNUMMER, melding.fødselsnummer.value)
        assertEquals(1.januar, melding.skjæringstidspunkt)
        assertEquals(avviksvurderingBehovId, melding.avviksvurderingBehovId)
        val expected = Sammenligningsgrunnlag(
            listOf(
                ArbeidsgiverInntekt(
                    ORGANISASJONSNUMMER.somArbeidsgiverref(), listOf(
                        ArbeidsgiverInntekt.MånedligInntekt(
                            inntekt = InntektPerMåned(value = 20000.0), måned = YearMonth.of(2023, 1),
                            fordel = Fordel("En fordel"),
                            beskrivelse = Beskrivelse("En beskrivelse"),
                            inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                        )
                    )
                )
            )
        )
        assertEquals(expected, melding.sammenligningsgrunnlag)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag uten løsning`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonUtenLøsning(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID()
            )
        )
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag som mangler årMåned`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekterForSammenligningsgrunnlag(null to emptyList())
            )
        )
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag som mangler inntektsliste`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekter = inntekterForSammenligningsgrunnlag(YearMonth.of(2023, 1) to null)
            )
        )
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser inn sammenligningsgrunnlag uten beskrivelse`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekter = inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt(fordel = null))
                )
            )
        )
        assertEquals(1, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag uten inntektstype`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt(inntektstype = null))
                )
            )
        )
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag uten orgnummer eller fødselsnummer`() {
        assertThrows<IllegalStateException> {
            testRapid.sendTestMessage(
                sammenligningsgrunnlagJsonMed(
                    fødselsnummer = FØDSELSNUMMER,
                    organisasjonsnummer = ORGANISASJONSNUMMER,
                    skjæringstidspunkt = 1.januar,
                    avviksvurderingBehovId = UUID.randomUUID(),
                    inntekterForSammenligningsgrunnlag(
                        YearMonth.of(2023, 1) to listOf(inntekt(arbeidsgiverMangler = true))
                    )
                )
            )
        }
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag uten beløp`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt(beløp = null))
                )
            )
        )
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag hvis inntektstype ikke er gyldig`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt(inntektstype = "NOE ANNET"))
                )
            )
        )
        assertEquals(0, messageHandler.messages.size)
    }

    @Test
    fun `Leser ikke inn sammenligningsgrunnlag hvis det ikke er final`() {
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekter = inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt(fordel = null))
                ),
                final = false
            )
        )
        testRapid.sendTestMessage(
            sammenligningsgrunnlagJsonMed(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                skjæringstidspunkt = 1.januar,
                avviksvurderingBehovId = UUID.randomUUID(),
                inntekter = inntekterForSammenligningsgrunnlag(
                    YearMonth.of(2023, 1) to listOf(inntekt(fordel = null))
                )
            )
        )
        assertEquals(1, messageHandler.messages.size)
    }

    private fun inntekterForSammenligningsgrunnlag(
        vararg inntekter: Pair<YearMonth?, List<Inntekt>?>
    ): List<InntektForSammenligningsgrunnlag> {
        return inntekter.map { (yearMonth, inntekter) ->
            InntektForSammenligningsgrunnlag(yearMonth, inntekter)
        }
    }

    private fun inntekt(
        beløp: Double? = 20000.0,
        inntektstype: String? = "LOENNSINNTEKT",
        arbeidsgiverMangler: Boolean = false,
        beskrivelse: String? = "En beskrivelse",
        fordel: String? = "En fordel"
    ): Inntekt {
        return Inntekt(
            beløp = beløp,
            inntektstype = inntektstype,
            orgnummer = if (arbeidsgiverMangler) null else ORGANISASJONSNUMMER,
            fødselsnummer = if (arbeidsgiverMangler) null else FØDSELSNUMMER,
            beskrivelse = beskrivelse,
            fordel = fordel
        )
    }

    private data class InntektForSammenligningsgrunnlag(
        val årMåned: YearMonth?,
        val inntektsliste: List<Inntekt>?
    )

    private data class Inntekt(
        val beløp: Double?,
        val inntektstype: String?,
        val orgnummer: String?,
        val fødselsnummer: String?,
        val beskrivelse: String?,
        val fordel: String?
    )

    private fun List<InntektForSammenligningsgrunnlag>.toJson(): String {
        return no.nav.helse.helpers.objectMapper.writeValueAsString(this)
    }

    private fun sammenligningsgrunnlagJsonMed(
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        avviksvurderingBehovId: UUID?,
        inntekter: List<InntektForSammenligningsgrunnlag> = inntekterForSammenligningsgrunnlag(
            YearMonth.of(2023, 1) to listOf(inntekt())
        ),
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
              "fødselsnummer": "$fødselsnummer",
              "organisasjonsnummer": "$organisasjonsnummer",
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "InntekterForSammenligningsgrunnlag": {
                "avviksvurderingBehovId": "$avviksvurderingBehovId",
                "skjæringstidspunkt": "$skjæringstidspunkt",
                "beregningStart": "2018-01",
                "beregningSlutt": "2018-02"
              },
              "@id": "ecfe47f6-2063-451a-b7e1-182490cc3153",
              "@opprettet": "2018-01-01T00:00:00.000",
              "@løsning": {
                "InntekterForSammenligningsgrunnlag": ${inntekter.toJson()}
              },
              "@final": $final
            }
        """.trimIndent()
        return json
    }

    private fun sammenligningsgrunnlagJsonUtenLøsning(
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        avviksvurderingBehovId: UUID,
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
              "fødselsnummer": "$fødselsnummer",
              "organisasjonsnummer": "$organisasjonsnummer",
              "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
              "InntekterForSammenligningsgrunnlag": {
                "avviksvurderingBehovId": "$avviksvurderingBehovId",
                "skjæringstidspunkt": "$skjæringstidspunkt",
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
