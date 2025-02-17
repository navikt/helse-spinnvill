@file:Suppress("SameParameterValue")

package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.*
import no.nav.helse.db.TestDatabase
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.helpers.januar
import no.nav.helse.helpers.objectMapper
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.test.*

internal class IntegrationTest {
    private val testRapid = TestRapid()
    private val database = TestDatabase.database()

    private val FØDSELSNUMMER = "12345678910"
    private val ORGANISASJONSNUMMER = "987654321"
    private val SKJÆRINGSTIDSPUNKT = 1.januar
    private val BEREGNINGSGRUNNLAG = AvviksvurderingDto.BeregningsgrunnlagDto(
        mapOf(
            Arbeidsgiverreferanse(ORGANISASJONSNUMMER) to OmregnetÅrsinntekt(600000.0),
        )
    )

    init {
        Mediator(VersjonAvKode("1.0.0"), testRapid, ::database)
    }

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
    }

    @Test
    fun `sender behov for sammenligningsgrunnlag`() {
        mottaAvviksvurderingBehov()

        assertEquals(1, testRapid.inspektør.size)
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(listOf("InntekterForSammenligningsgrunnlag"), testRapid.inspektør.field(0, "@behov").map { it.asText() })
    }

    @Test
    fun `sender behovløsningUtenVurdering hvis det ikke gjøres ny avviksvurdering`() {
        mottaAvviksvurderingBehov()
        mottaSammenligningsgrunnlag()

        testRapid.reset()

        mottaAvviksvurderingBehov()
        val behov = database.finnUbehandletAvviksvurderingBehov(FØDSELSNUMMER.somFnr(), SKJÆRINGSTIDSPUNKT)
        assertNull(behov) // behovet er markert ferdigstilt

        assertEquals(1, testRapid.inspektør.size)
        assertEquals(1, testRapid.inspektør.behov("Avviksvurdering").size)

        val løsningNode = testRapid.inspektør.behov("Avviksvurdering").single().path("@løsning").path("Avviksvurdering")
        assertEquals("TrengerIkkeNyVurdering", løsningNode.path("utfall").asText())
    }

    @Test
    fun `sender behovløsningMedVurdering hvis det gjøres ny avviksvurdering og avviket er mer enn tillatt avvik`() {
        mottaAvviksvurderingBehov()
        mottaSammenligningsgrunnlag()

        testRapid.reset()

        mottaAvviksvurderingBehov( beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(ORGANISASJONSNUMMER.somArbeidsgiverref() to OmregnetÅrsinntekt(900000.0))
        ))
        assertEquals(2, testRapid.inspektør.size)
        assertEquals(1, testRapid.inspektør.behov("Avviksvurdering").size)
        assertEquals(1, testRapid.inspektør.hendelser("subsumsjon").size)
        assertEquals("2", testRapid.inspektør.hendelser("subsumsjon").single()["subsumsjon"].get("ledd").asText())
        assertEquals("1", testRapid.inspektør.hendelser("subsumsjon").single()["subsumsjon"].get("punktum").asText())

        val løsningNode = testRapid.inspektør.behov("Avviksvurdering").single().path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode.path("utfall").asText())
    }

    @Test
    fun `sender behovløsningMedVurdering hvis det gjøres ny avviksvurdering og avviket er innenfor tillatt avvik`() {
        mottaAvviksvurderingBehov()
        mottaSammenligningsgrunnlag()

        testRapid.reset()

        mottaAvviksvurderingBehov( beregningsgrunnlag = AvviksvurderingDto.BeregningsgrunnlagDto(
            mapOf(ORGANISASJONSNUMMER.somArbeidsgiverref() to OmregnetÅrsinntekt(700000.0))
        ))
        assertEquals(3, testRapid.inspektør.size)
        assertEquals(1, testRapid.inspektør.behov("Avviksvurdering").size)
        assertEquals(2, testRapid.inspektør.hendelser("subsumsjon").size)

        val løsningNode = testRapid.inspektør.behov("Avviksvurdering").single().path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode.path("utfall").asText())
        assertTrue(testRapid.inspektør.hendelser("subsumsjon").any {it["subsumsjon"].get("ledd").asText() == "2" && it["subsumsjon"].get("punktum").asText() == "1" })
        assertTrue(testRapid.inspektør.hendelser("subsumsjon").any {it["subsumsjon"].get("ledd").asText() == "1" && it["subsumsjon"].path("punktum").isMissingOrNull()})
    }


    private fun mottaAvviksvurderingBehov(vedtaksperiodeId: UUID = UUID.randomUUID(), beregningsgrunnlag: AvviksvurderingDto.BeregningsgrunnlagDto = BEREGNINGSGRUNNLAG) {
        testRapid.sendTestMessage(
            avviksvurderingBehov(
                FØDSELSNUMMER,
                ORGANISASJONSNUMMER,
                SKJÆRINGSTIDSPUNKT,
                vedtaksperiodeId,
                beregningsgrunnlag
            )
        )
    }

    private fun mottaSammenligningsgrunnlag(årsinntekt: Double = 600000.0, antallMåneder: Int = 12) {
        val behov = testRapid.inspektør.sisteBehovAvType("InntekterForSammenligningsgrunnlag") as? ObjectNode
        assertNotNull(behov)

        val inntekterForSammenligningsgrunnlag =
            objectMapper.convertValue(mapOf("InntekterForSammenligningsgrunnlag" to List(antallMåneder) {
                mapOf(
                    "årMåned" to YearMonth.of(2018, it + 1),
                    "inntektsliste" to listOf(
                        mapOf<String, Any>(
                            "beløp" to årsinntekt / antallMåneder,
                            "inntektstype" to "LOENNSINNTEKT",
                            "orgnummer" to ORGANISASJONSNUMMER
                        )
                    )
                )
            }), JsonNode::class.java)

        val løsning = behov
            .set<ObjectNode>("@løsning", inntekterForSammenligningsgrunnlag)
            .put("@final", true)

        testRapid.sendTestMessage(objectMapper.writeValueAsString(løsning))
    }

    private fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    private fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.sisteBehovAvType(vararg behov: String) =
        hendelser("behov").lastOrNull {
            it.path("@behov").map(JsonNode::asText).containsAll(behov.toList()) && !it.hasNonNull("@løsning")
        }

    private fun TestRapid.RapidInspector.behov(behov: String) =
        hendelser("behov")
            .filter { it.path("@behov").map(JsonNode::asText).containsAll(listOf(behov)) }

    private fun avviksvurderingBehov(
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        vedtaksperiodeId: UUID,
        beregningsgrunnlagDto: AvviksvurderingDto.BeregningsgrunnlagDto
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
                    "vedtaksperiodeId": "$vedtaksperiodeId",
                    "skjæringstidspunkt": "$skjæringstidspunkt",
                    "vilkårsgrunnlagId": "87b9339d-a67d-49b0-af36-c93d6f9249ae",
                    "omregnedeÅrsinntekter": ${beregningsgrunnlagDto.toJson()} 
                },
                "@id": "ba376523-62b1-49d7-8647-f902c739b634",
                "@opprettet": "2018-01-01T00:00:00.000"
            }
        """.trimIndent()
        return json
    }

    private fun AvviksvurderingDto.BeregningsgrunnlagDto.toJson(): String {
        return objectMapper.writeValueAsString(this.omregnedeÅrsinntekter.map {
            mapOf(
                "organisasjonsnummer" to it.key,
                "beløp" to it.value
            )
        })
    }

}
