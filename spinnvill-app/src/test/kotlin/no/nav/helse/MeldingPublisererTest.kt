package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.avviksvurdering.*
import no.nav.helse.helpers.*
import no.nav.helse.kafka.asUUID
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class MeldingPublisererTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val organisasjonsnummer = Arbeidsgiverreferanse("987654321")
    private val vedtaksperiodeId = UUID.randomUUID()
    private val vilkårsgrunnlagId = UUID.randomUUID()
    private val behovId = UUID.randomUUID()
    private val versjonAvKode = VersjonAvKode("hello")
    private val beregningsgrunnlag = dummyBeregningsgrunnlag
    private val sammenligningsgrunnlag = dummySammenligningsgrunnlag
    private val testRapid = TestRapid()
    private val meldingPubliserer = MeldingPubliserer(testRapid, avviksvurderingBehov(), versjonAvKode)

    @Test
    fun `lag sammenligningsgrunnlag-behov`() {
        val skjæringstidspunkt = 1.januar
        val beregningsperiodeFom = januar(2017)
        val beregningsperiodeTom = desember(2017)
        meldingPubliserer.behovForSammenligningsgrunnlag(BehovForSammenligningsgrunnlag(skjæringstidspunkt, beregningsperiodeFom, beregningsperiodeTom))
        meldingPubliserer.sendMeldinger()

        val message = testRapid.inspektør.message(0)
        val behovdata = message["InntekterForSammenligningsgrunnlag"]

        assertEquals("behov", message["@event_name"].asText())
        assertEquals(listOf("InntekterForSammenligningsgrunnlag"), message["@behov"].map { it.asText() })
        assertEquals(beregningsperiodeFom, behovdata["beregningStart"].asYearMonth())
        assertEquals(beregningsperiodeTom, behovdata["beregningSlutt"].asYearMonth())
        assertEquals(skjæringstidspunkt, behovdata["skjæringstidspunkt"].asLocalDate())
        assertEquals(behovId, behovdata["avviksvurderingBehovId"].asUUID())
    }

    @Test
    fun `standardfelter blir lagt på alle utgående meldinger`() {
        val skjæringstidspunkt = 1.januar
        val beregningsperiodeFom = januar(2017)
        val beregningsperiodeTom = desember(2017)
        meldingPubliserer.behovForSammenligningsgrunnlag(BehovForSammenligningsgrunnlag(skjæringstidspunkt, beregningsperiodeFom, beregningsperiodeTom))
        meldingPubliserer.`8-30 ledd 1`(dummyBeregningsgrunnlag)
        meldingPubliserer.`8-30 ledd 2 punktum 1`(avviksvurdering = avviksvurdering(
            false, 26.0, beregningsgrunnlag, sammenligningsgrunnlag, 25.0
        ))
        meldingPubliserer.sendMeldinger()


        assertEquals(3, testRapid.inspektør.size)
        testRapid.inspektør.meldinger().forEach {
            assertEquals(fødselsnummer, it.path("fødselsnummer").asText().somFnr())
            assertEquals(organisasjonsnummer, it.path("organisasjonsnummer").asText().somArbeidsgiverref())
            assertEquals(skjæringstidspunkt, it.path("skjæringstidspunkt").asLocalDate())
            assertEquals(vedtaksperiodeId, it.path("vedtaksperiodeId").asUUID())
        }
    }

    @Test
    fun `lag subsumsjonsmelding for fastsettelse etter hovedregel - 8-30 ledd 1`() {

        meldingPubliserer.`8-30 ledd 1`(dummyBeregningsgrunnlag)
        meldingPubliserer.sendMeldinger()

        val message = testRapid.inspektør.message(0)

        assertEquals("subsumsjon", message["@event_name"].asText())

        val subsumsjon = message["subsumsjon"]

        assertEquals("8-30", subsumsjon["paragraf"].asText())
        assertNull(subsumsjon["bokstav"])
        assertNull(subsumsjon["punktum"])
        assertEquals(1, subsumsjon["ledd"].asInt())
        assertEquals("VILKAR_BEREGNET", subsumsjon["utfall"].asText())
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asText())
        assertEquals("2019-01-01", subsumsjon["lovverksversjon"].asText())

        assertPresent(subsumsjon["input"])
        assertPresent(subsumsjon["output"])
        val input = subsumsjon["input"]
        val output = subsumsjon["output"]

        assertPresent(input["omregnedeÅrsinntekter"])
        val omregnedeÅrsinntekterNode = input["omregnedeÅrsinntekter"]
        assertEquals("a1", omregnedeÅrsinntekterNode[0]["arbeidsgiverreferanse"].asText())
        assertEquals(600000.0, omregnedeÅrsinntekterNode[0]["inntekt"].asDouble())

        assertPresent(output["grunnlagForSykepengegrunnlag"])
        assertEquals(600000.0, output["grunnlagForSykepengegrunnlag"].asDouble())
    }

    @Test
    fun `lager subsumsjonsmelding for 8-30 ledd 2 punktum 1`() {
        meldingPubliserer.`8-30 ledd 2 punktum 1`(avviksvurdering = avviksvurdering(
            false, 26.0, beregningsgrunnlag, sammenligningsgrunnlag, 25.0
        ))
        meldingPubliserer.sendMeldinger()

        val message = testRapid.inspektør.message(0)

        assertEquals("subsumsjon", message["@event_name"].asText())

        val subsumsjon = message["subsumsjon"]
        assertEquals("8-30", subsumsjon["paragraf"].asText())
        assertNull(subsumsjon["bokstav"])
        assertEquals(2, subsumsjon["ledd"].asInt())
        assertEquals(1, subsumsjon["punktum"].asInt())
        assertEquals("VILKAR_BEREGNET", subsumsjon["utfall"].asText())
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asText())
        assertEquals("2019-01-01", subsumsjon["lovverksversjon"].asText())

        assertPresent(subsumsjon["input"])
        val input = subsumsjon["input"]
        val output = subsumsjon["output"]

        assertPresent(input["grunnlagForSykepengegrunnlag"])

        val beregningsgrunnlagNode = input["grunnlagForSykepengegrunnlag"]
        assertEquals(600000.0, beregningsgrunnlagNode["totalbeløp"].asDouble())
        assertPresent(beregningsgrunnlagNode["omregnedeÅrsinntekter"])

        val omregnedeÅrsinntekterNode = beregningsgrunnlagNode["omregnedeÅrsinntekter"]
        assertEquals("a1", omregnedeÅrsinntekterNode[0]["arbeidsgiverreferanse"].asText())
        assertEquals(600000.0, omregnedeÅrsinntekterNode[0]["inntekt"].asDouble())

        assertPresent(input["sammenligningsgrunnlag"])

        val sammenligningsgrunnlagNode = input["sammenligningsgrunnlag"]
        assertEquals(600000.0, sammenligningsgrunnlagNode["totalbeløp"].asDouble())
        assertPresent(sammenligningsgrunnlagNode["innrapporterteMånedsinntekter"])

        val månedligeInntekterNode = sammenligningsgrunnlagNode["innrapporterteMånedsinntekter"]
        assertEquals(12, månedligeInntekterNode.size())
        månedligeInntekterNode.forEachIndexed { it, node ->
            assertPresent(node["måned"])
            assertEquals(YearMonth.of(2018, it + 1), node["måned"].asYearMonth())
            assertPresent(node["inntekter"])
            val månedligInntekt = månedligeInntekterNode[it]["inntekter"][0]
            assertEquals("a1", månedligInntekt["arbeidsgiverreferanse"].asText())
            assertEquals(50000.0, månedligInntekt["inntekt"].asDouble())
            assertEquals("En fordel", månedligInntekt["fordel"].asText())
            assertEquals("En beskrivelse", månedligInntekt["beskrivelse"].asText())
            assertEquals("LØNNSINNTEKT", månedligInntekt["inntektstype"].asText())
        }

        assertEquals(25.0, input["maksimaltTillattAvvikPåÅrsinntekt"].asDouble())
        assertEquals(26.0, output["avviksprosent"].asDouble())
        assertEquals(false, output["harAkseptabeltAvvik"].asBoolean())
    }

    @Test
    fun `svarer ut behov med løsning uten vurdering`() {
        val avviksvurderingId = UUID.randomUUID()
        meldingPubliserer.behovløsningUtenVurdering(avviksvurderingId)
        meldingPubliserer.sendMeldinger()

        assertEquals(1, testRapid.inspektør.behov("Avviksvurdering").size)
        val behovNode = testRapid.inspektør.behov("Avviksvurdering").single()

        val løsningNode = behovNode.path("@løsning").path("Avviksvurdering")
        assertEquals("TrengerIkkeNyVurdering", løsningNode.path("utfall").asText())
        assertNotNull(løsningNode.get("avviksvurderingId"))
    }

    @Test
    fun `svarer ut behov med løsning med vurdering`() {
        meldingPubliserer.behovløsningMedVurdering(vurdering = avviksvurdering(
            false, 26.0, beregningsgrunnlag, sammenligningsgrunnlag, 25.0
        ))
        meldingPubliserer.sendMeldinger()

        assertEquals(1, testRapid.inspektør.behov("Avviksvurdering").size)
        val behovNode = testRapid.inspektør.behov("Avviksvurdering").single()

        val løsningNode = behovNode.path("@løsning").path("Avviksvurdering")
        assertEquals("NyVurderingForetatt", løsningNode.path("utfall").asText())
        assertNotNull(løsningNode.get("avviksvurderingId"))
        assertNotNull(løsningNode.get("harAkseptabeltAvvik"))
        assertNotNull(løsningNode.get("maksimaltTillattAvvik"))
        assertNotNull(løsningNode.get("opprettet"))

        val beregningsgrunnlagNode = løsningNode.path("beregningsgrunnlag")
        assertNotNull(beregningsgrunnlagNode.get("totalbeløp"))
        val omregnedeÅrsinntekter = beregningsgrunnlagNode.path("omregnedeÅrsinntekter") as ArrayNode
        assertFalse(omregnedeÅrsinntekter.isEmpty)
        omregnedeÅrsinntekter.forEach {
            assertNotNull(it.get("arbeidsgiverreferanse"))
            assertNotNull(it.get("beløp"))
        }

        val sammenligningsgrunnlagNode = løsningNode.path("sammenligningsgrunnlag")
        assertNotNull(sammenligningsgrunnlagNode.get("totalbeløp"))
        val innrapporterteInntekterNode = sammenligningsgrunnlagNode.path("innrapporterteInntekter") as ArrayNode
        assertFalse(innrapporterteInntekterNode.isEmpty)
        innrapporterteInntekterNode.forEach { innrapportertInntekt ->
            assertNotNull(innrapportertInntekt.get("arbeidsgiverreferanse"))
            val inntekterNode = innrapportertInntekt.path("inntekter") as ArrayNode
            assertFalse(inntekterNode.isEmpty)
            inntekterNode.forEach { inntekt ->
                assertNotNull(inntekt.get("årMåned"))
                assertNotNull(inntekt.get("beløp"))
            }
        }
    }

    private fun avviksvurderingBehovJsonMap(
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate
    ): Map<String, Any> {
        @Language("JSON")
        val json = """
            {
                "@event_name": "behov",
                "@behovId": "c64a73be-7337-4f25-8923-94f355c23d76",
                "@behov": [
                    "Avviksvurdering"
                ],
                "fødselsnummer": "$fødselsnummer",
                "organisasjonsnummer": "$organisasjonsnummer",
                "vedtaksperiodeId": "d6a1575f-a241-4338-baea-26df557f7506",
                "Avviksvurdering": {
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
        return objectMapper.readValue<Map<String, Any>>(json)
    }

    private fun avviksvurderingBehov(): AvviksvurderingBehov {
        return AvviksvurderingBehov.nyttBehov(
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            behovId = behovId,
            skjæringstidspunkt = 1.januar,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            beregningsgrunnlag = beregningsgrunnlag,
            json = avviksvurderingBehovJsonMap(organisasjonsnummer = organisasjonsnummer.value, fødselsnummer = fødselsnummer.value, skjæringstidspunkt = 1.januar)
        )
    }

    private fun avviksvurdering(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double,
    ): Avviksvurdering {
        return Avviksvurdering(
            id = UUID.randomUUID(),
            harAkseptabeltAvvik = harAkseptabeltAvvik,
            avviksprosent = avviksprosent,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            maksimaltTillattAvvik = maksimaltTillattAvvik,
        )
    }

    private fun assertPresent(jsonNode: JsonNode?) {
        assertNotNull(jsonNode) { "Forventer at noden ikke er null" }
        jsonNode?.isMissingOrNull()?.let { assertFalse(it) { "Forventer at noden ikke mangler" } }
    }

    private fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    private fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.behov(behov: String) =
        hendelser("behov")
            .filter { it.path("@behov").map(JsonNode::asText).containsAll(listOf(behov)) }
}



