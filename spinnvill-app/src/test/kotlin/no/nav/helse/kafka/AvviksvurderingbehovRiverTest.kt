package no.nav.helse.kafka

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.SammenligningsgrunnlagLøsning
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvviksvurderingbehovRiverTest {

    private val testRapid = TestRapid()

    private val messageHandler = object : MessageHandler {
        val behov = mutableListOf<AvviksvurderingBehov>()

        override fun håndter(behov: AvviksvurderingBehov) {
            this.behov.add(behov)
        }

        override fun håndter(løsning: SammenligningsgrunnlagLøsning) {}
    }

    private companion object {
        private const val FØDSELSNUMMER = "12345678910"
        private const val ORGANISASJONSNUMMER = "987654321"
        private val skjæringstidspunkt = 1.januar
    }

    init {
        AvviksvurderingbehovRiver(testRapid, messageHandler)
    }

    @Test
    fun `les inn behov om avviksvurdering`() {
        testRapid.sendTestMessage(avviksvurderingBehov(skjæringstidspunkt = skjæringstidspunkt))
        val behov = messageHandler.behov.single()

        assertEquals(FØDSELSNUMMER, behov.fødselsnummer.value)
        assertEquals(skjæringstidspunkt, behov.skjæringstidspunkt)
        assertEquals(
            Beregningsgrunnlag.opprett(
                mapOf(
                    Arbeidsgiverreferanse(ORGANISASJONSNUMMER) to OmregnetÅrsinntekt(
                        500000.0
                    ), Arbeidsgiverreferanse("000000000") to OmregnetÅrsinntekt(200000.20)
                )
            ), behov.beregningsgrunnlag
        )
    }

    @Test
    fun `leser ikke inn behov om avviksvurdering med løsning`() {
        testRapid.sendTestMessage(avviksvurderingJsonMedLøsning(FØDSELSNUMMER, ORGANISASJONSNUMMER, skjæringstidspunkt))
        assertEquals(0, messageHandler.behov.size)
    }

    private fun avviksvurderingBehov(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
        skjæringstidspunkt: LocalDate
    ): String {
        @Language("JSON")
        val json = """
            {
                "@event_name": "behov",
                "@behovId": "c64a73be-7337-4f25-8923-94f355c23d76",
                "@behov": [
                    "Avviksvurdering"
                ],
                "fødselsnummer": "$fødselsnummer",
                "Avviksvurdering": {
                    "organisasjonsnummer": "$organisasjonsnummer",
                    "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
                    "skjæringstidspunkt": "$skjæringstidspunkt",
                    "vilkårsgrunnlagId": "87b9339d-a67d-49b0-af36-c93d6f9249ae",
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

    private fun avviksvurderingJsonNode(
        fødselsnummer: String, organisasjonsnummer: String, skjæringstidspunkt: LocalDate
    ) = avviksvurderingBehov(fødselsnummer, organisasjonsnummer, skjæringstidspunkt)
        .let(objectMapper::readTree) as ObjectNode

    private fun avviksvurderingJsonMedLøsning(
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate
    ) = avviksvurderingJsonNode(fødselsnummer, organisasjonsnummer, skjæringstidspunkt)
        .med("@løsning" to "{}")
        .let(objectMapper::writeValueAsString)

    private fun ObjectNode.med(vararg felter: Pair<String, Any>): ObjectNode {
        felter.forEach { (key, value) -> replace(key, objectMapper.valueToTree(value)) }
        return this
    }
}
