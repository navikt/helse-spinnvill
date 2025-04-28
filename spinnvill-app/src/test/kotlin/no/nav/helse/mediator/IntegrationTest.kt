@file:Suppress("SameParameterValue")

package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.db.TestDatabase
import no.nav.helse.helpers.lagFødselsnummer
import no.nav.helse.helpers.lagOrganisasjonsnummer
import no.nav.helse.helpers.objectMapper
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class IntegrationTest {
    private val testRapid = TestRapid()
    private val database = TestDatabase.database()

    init {
        Mediator(VersjonAvKode("1.0.0"), testRapid, ::database)
    }

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
    }

    @Test
    fun `be om sammenligningsgrunnlag når det ikke er gjort noen avviksvurdering tidligere`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)

        assertEquals(1, testRapid.inspektør.size)
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(listOf("InntekterForSammenligningsgrunnlag"), testRapid.inspektør.field(0, "@behov").map { it.asText() })
    }

    @Test
    fun `ignorerer historikk og foretar dermed ny avviksvurdering dersom gjeldende avviksvurderingsgrunnlag er av type Infotrygd`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)
        fakeGammeltInfotrygdAvviksvurderinggrunnlag()
        testRapid.reset()
        mottaAvviksvurderingBehov(600_000.0)

        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertEquals("behov", sisteMelding["@event_name"].asText())
        assertEquals(listOf("InntekterForSammenligningsgrunnlag"), sisteMelding["@behov"].map { it.asText() })
    }

    @Test
    fun `sender behovløsningUtenVurdering hvis det ikke gjøres ny avviksvurdering`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)

        mottaAvviksvurderingBehov(600_000.0)

        val behov = database.finnUbehandletAvviksvurderingBehov(fødselsnummer.somFnr(), skjæringstidspunkt)
        assertNull(behov) // behovet er markert ferdigstilt

        val løsningNode = testRapid.inspektør.behov("Avviksvurdering").last().path("@løsning").path("Avviksvurdering")
        assertEquals("TrengerIkkeNyVurdering", løsningNode.path("utfall").asText())
    }

    @Test
    fun `sender behovløsningMedVurdering hvis det gjøres ny avviksvurdering og avviket er mer enn tillatt avvik`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)

        mottaAvviksvurderingBehov(900_000.0)

        assertEquals("2", testRapid.inspektør.hendelser("subsumsjon").last()["subsumsjon"].get("ledd").asText())
        assertEquals("1", testRapid.inspektør.hendelser("subsumsjon").last()["subsumsjon"].get("punktum").asText())

        val løsningNode = testRapid.inspektør.behov("Avviksvurdering").last().path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode.path("utfall").asText())
    }

    @Test
    fun `sender behovløsningMedVurdering hvis det gjøres ny avviksvurdering og avviket er innenfor tillatt avvik`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)

        mottaAvviksvurderingBehov(700_000.0)

        val løsningNode = testRapid.inspektør.behov("Avviksvurdering").last().path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode.path("utfall").asText())
        assertTrue(testRapid.inspektør.hendelser("subsumsjon").any {it["subsumsjon"].get("ledd").asText() == "2" && it["subsumsjon"].get("punktum").asText() == "1" })
        assertTrue(testRapid.inspektør.hendelser("subsumsjon").any {it["subsumsjon"].get("ledd").asText() == "1" && it["subsumsjon"].path("punktum").isMissingOrNull()})
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag bare er litt forskjellig`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)

        mottaAvviksvurderingBehov(600000.1)
        mottaAvviksvurderingBehov(599999.999999994)

        val løsningNode2 = testRapid.inspektør.behov("Avviksvurdering")[1].path("@løsning").path("Avviksvurdering")
        val løsningNode3 = testRapid.inspektør.behov("Avviksvurdering")[2].path("@løsning").path("Avviksvurdering")
        assertEquals("TrengerIkkeNyVurdering", løsningNode2.path("utfall").asText())
        assertEquals("TrengerIkkeNyVurdering", løsningNode3.path("utfall").asText())
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag er forskjellig`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)

        mottaAvviksvurderingBehov(500000.0)

        val løsningNode2 = testRapid.inspektør.behov("Avviksvurdering")[1].path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode2.path("utfall").asText())
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og mottar beregningsgrunnlag med beløp 0 kr`() = medTestContext(1 jan 2018) {
        mottaAvviksvurderingBehov(600_000.0)
        mottaSammenligningsgrunnlag(600_000.0)

        mottaAvviksvurderingBehov(0.0)

        val løsningNode2 = testRapid.inspektør.behov("Avviksvurdering")[1].path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode2.path("utfall").asText())
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

    private fun medTestContext(
        skjæringstidspunkt: LocalDate,
        antallArbeidsgivere: Int = 1,
        whenBlock: TestContext.() -> Unit
    ) {
        val arbeidsgivere = List(antallArbeidsgivere) {
            Arbeidsgiverreferanse(lagOrganisasjonsnummer())
        }
        whenBlock(
            TestContext(
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgivere = arbeidsgivere
            )
        )
    }

    private fun TestContext.mottaAvviksvurderingBehov(omregnetÅrsinntekt: Double) {
        testRapid.sendTestMessage(
            avviksvurderingBehov(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                vedtaksperiodeId = UUID.randomUUID(),
                beregningsgrunnlag = nyttBeregningsgrunnlag(omregnetÅrsinntekt)
            )
        )
    }

    private fun TestContext.mottaSammenligningsgrunnlag(innrapportertÅrsinntektÅrlig: Double) {
        val behov = testRapid.inspektør.sisteBehovAvType("InntekterForSammenligningsgrunnlag") as? ObjectNode
        assertNotNull(behov)

        val gruppertPåÅrMåned = this.nyttSammenligningsgrunnlag(innrapportertÅrsinntektÅrlig).inntekter
            .flatMap { (key, values) ->
                values.map { key to it }
            }
            .groupBy { it.second.måned }

        val inntekterForSammenligningsgrunnlag =
            objectMapper.convertValue(mapOf("InntekterForSammenligningsgrunnlag" to gruppertPåÅrMåned.map {
                mapOf(
                    "årMåned" to it.key,
                    "inntektsliste" to it.value.map { (organisasjonsnummer, månedligInntekt) ->
                        mapOf(
                            "beløp" to månedligInntekt.inntekt.value,
                            "inntektstype" to "LOENNSINNTEKT",
                            "orgnummer" to organisasjonsnummer.value,
                        )
                    }
                )
            }), JsonNode::class.java)

        val løsning = behov
            .set<ObjectNode>("@løsning", inntekterForSammenligningsgrunnlag)
            .put("@final", true)

        testRapid.sendTestMessage(objectMapper.writeValueAsString(løsning))
    }

    private fun avviksvurderingBehov(
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        vedtaksperiodeId: UUID,
        beregningsgrunnlag: Beregningsgrunnlag
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
                    "omregnedeÅrsinntekter": ${beregningsgrunnlag.toJson()} 
                },
                "@id": "ba376523-62b1-49d7-8647-f902c739b634",
                "@opprettet": "2018-01-01T00:00:00.000"
            }
        """.trimIndent()
        return json
    }

    private fun Beregningsgrunnlag.toJson(): String {
        return objectMapper.writeValueAsString(this.omregnedeÅrsinntekter.map {
            mapOf(
                "organisasjonsnummer" to it.key,
                "beløp" to it.value
            )
        })
    }

    private fun TestContext.fakeGammeltInfotrygdAvviksvurderinggrunnlag() {
        val conn = database.datasource().connection
        val stmt = conn.prepareStatement("""
            INSERT INTO avviksvurdering(id, fødselsnummer, skjæringstidspunkt, opprettet, kilde) 
            VALUES (?::uuid, ?, ?::timestamp, ?::timestamp, ?)
            """
        )
        stmt.setString(1, UUID.randomUUID().toString())
        stmt.setString(2, fødselsnummer)
        stmt.setString(3, skjæringstidspunkt.toString())
        stmt.setString(4, LocalDateTime.now().toString())
        stmt.setString(5, "INFOTRYGD")
        stmt.executeUpdate()
        conn.close()
    }

    private class TestContext(
        val skjæringstidspunkt: LocalDate,
        val arbeidsgivere: List<Arbeidsgiverreferanse>
    ) {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()

        lateinit var beregningsgrunnlag: Beregningsgrunnlag
            private set

        lateinit var sammenligningsgrunnlag: Sammenligningsgrunnlag
            private set

        fun nyttBeregningsgrunnlag(omregnetÅrsinntekt: Double): Beregningsgrunnlag {
            beregningsgrunnlag = Beregningsgrunnlag(
                arbeidsgivere.map {
                    it to OmregnetÅrsinntekt(omregnetÅrsinntekt)
                }.toMap()
            )
            return beregningsgrunnlag
        }

        fun nyttSammenligningsgrunnlag(innrapportertÅrsinntekt: Double): Sammenligningsgrunnlag {
            val sluttmåned = YearMonth.from(skjæringstidspunkt.minusMonths(1))
            val startmåned = YearMonth.from(sluttmåned.minusMonths(12))
            val yearMonthRange = yearMonthRange(startmåned, sluttmåned)
            sammenligningsgrunnlag = Sammenligningsgrunnlag(
                inntekter =
                arbeidsgivere.map {
                    val månedligBeløp = innrapportertÅrsinntekt / yearMonthRange.size
                    ArbeidsgiverInntekt(
                        arbeidsgiverreferanse = it,
                        inntekter = yearMonthRange.map { årMåned ->
                            ArbeidsgiverInntekt.MånedligInntekt(
                                InntektPerMåned(månedligBeløp),
                                årMåned,
                                fordel = null,
                                beskrivelse = null,
                                ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                            )
                        }
                    )
                }
            )
            return sammenligningsgrunnlag
        }
    }

    private companion object {
        private fun yearMonthRange(start: YearMonth, endInclusive: YearMonth): List<YearMonth> = sequence {
            var current = start
            while (!current.isAfter(endInclusive)) {
                yield(current)
                current = current.plusMonths(1)
            }
        }.toList()
    }
}
