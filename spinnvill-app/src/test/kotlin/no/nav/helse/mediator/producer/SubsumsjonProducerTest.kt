package no.nav.helse.mediator.producer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.Fødselsnummer
import no.nav.helse.VersjonAvKode
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SubsumsjonProducerTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val vedtaksperiodeId = UUID.randomUUID()
    private val testRapid = TestRapid()
    private val versjonAvKode = VersjonAvKode("hello")
    private val subsumsjonProducer = SubsumsjonProducer(fødselsnummer, versjonAvKode, testRapid)

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `produser subsumsjonsmelding hvis avviket er akseptabelt`() {
        subsumsjonProducer.avvikVurdert(true, 20.0)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `produser subsumsjonsmelding hvis avviket ikke er akseptabelt`() {
        subsumsjonProducer.avvikVurdert(false, 42.0)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `subsumsjonskø tømmes etter hver finalize`() {
        subsumsjonProducer.avvikVurdert(false, 26.0)
        subsumsjonProducer.finalize()
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `ikke send ut subsumsjonsmeldinger før finalize blir kalt`() {
        subsumsjonProducer.avvikVurdert(false, 26.0)
        assertEquals(0, testRapid.inspektør.size)
        subsumsjonProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `produserer riktig format på subsumsjonsmelding`() {
        subsumsjonProducer.avvikVurdert(false, 26.0)
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
    }

    private fun assertPresent(jsonNode: JsonNode?) {
        assertNotNull(jsonNode) { "Forventer at noden ikke er null" }
        jsonNode?.isMissingOrNull()?.let { assertFalse(it) { "Forventer at noden ikke mangler" } }
    }
}