package no.nav.helse.mediator.producer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.*
import no.nav.helse.helpers.beregningsgrunnlag
import no.nav.helse.helpers.dummyBeregningsgrunnlag
import no.nav.helse.helpers.dummySammenligningsgrunnlag
import no.nav.helse.helpers.sammenligningsgrunnlag
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

internal class SubsumsjonProducerTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val organisasjonsnummer = Arbeidsgiverreferanse("987654321")
    private val vedtaksperiodeId = UUID.randomUUID()
    private val vilkårsgrunnlagId = UUID.randomUUID()
    private val testRapid = TestRapid()
    private val versjonAvKode = VersjonAvKode("hello")
    private val subsumsjonProducer = SubsumsjonProducer(
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        versjonAvKode = versjonAvKode,
        rapidsConnection = testRapid
    )

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `produser subsumsjonsmelding hvis avviket er akseptabelt`() {
        subsumsjonProducer.avvikVurdert(true, 20.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `produser subsumsjonsmelding hvis avviket ikke er akseptabelt`() {
        subsumsjonProducer.avvikVurdert(false, 42.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `subsumsjonskø tømmes etter hver finalize`() {
        subsumsjonProducer.avvikVurdert(false, 26.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        subsumsjonProducer.finalize()
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `ikke send ut subsumsjonsmeldinger før finalize blir kalt`() {
        subsumsjonProducer.avvikVurdert(false, 26.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        assertEquals(0, testRapid.inspektør.size)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `produserer riktig format på subsumsjonsmelding`() {
        subsumsjonProducer.avvikVurdert(false, 26.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
        val message = testRapid.inspektør.message(0)
        assertEquals("subsumsjon", message["@event_name"].asText())
        assertPresent(message["@id"])
        assertPresent(message["@opprettet"])
        assertPresent(message["subsumsjon"])
        val subsumsjon = message["subsumsjon"]
        assertEquals(fødselsnummer.value, subsumsjon["fodselsnummer"].asText())
        assertPresent(subsumsjon["id"])
        assertPresent(subsumsjon["tidsstempel"])
        assertEquals("spinnvill", subsumsjon["kilde"].asText())
        assertEquals("1.0.0", subsumsjon["versjon"].asText())
        assertPresent(subsumsjon["paragraf"])
        assertPresent(subsumsjon["lovverk"])
        assertPresent(subsumsjon["lovverksversjon"])
        assertPresent(subsumsjon["utfall"])
        assertPresent(subsumsjon["input"])
        assertPresent(subsumsjon["output"])
        assertPresent(subsumsjon["sporing"])
        assertEquals(versjonAvKode.value, subsumsjon["versjonAvKode"].asText())

        assertPresent(subsumsjon["sporing"])

        val sporing = subsumsjon["sporing"]
        assertEquals(listOf(vedtaksperiodeId.toString()), sporing["vedtaksperiode"].map { it.asText() })
        assertEquals(listOf(vilkårsgrunnlagId.toString()), sporing["vilkårsgrunnlag"].map { it.asText() })
        assertEquals(listOf(organisasjonsnummer.value), sporing["organisasjonsnummer"].map { it.asText() })
    }

    @Test
    fun `lag subsumsjonsmelding for avviksvurdering`() {
        val beregningsgrunnlag = beregningsgrunnlag("a1" to 600000.0)
        val sammenligningsgrunnlag = sammenligningsgrunnlag("a1" to 50000.0)
        subsumsjonProducer.avvikVurdert(false, 26.0, beregningsgrunnlag, sammenligningsgrunnlag, 25.0)
        subsumsjonProducer.finalize()
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

    private fun assertPresent(jsonNode: JsonNode?) {
        assertNotNull(jsonNode) { "Forventer at noden ikke er null" }
        jsonNode?.isMissingOrNull()?.let { assertFalse(it) { "Forventer at noden ikke mangler" } }
    }
}