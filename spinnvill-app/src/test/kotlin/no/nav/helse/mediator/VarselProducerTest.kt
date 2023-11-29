package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.Fødselsnummer
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class VarselProducerTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val vedtaksperiodeId = UUID.randomUUID()
    private val testRapid = TestRapid()
    private val varselProducer = VarselProducer(fødselsnummer, vedtaksperiodeId, testRapid)

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `ikke produser varsel hvis avviket er akseptabelt`() {
        varselProducer.avvikVurdert(true, 20.0)
        varselProducer.finalize()
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `varselkø tømmes etter hver finalize`() {
        varselProducer.avvikVurdert(false, 26.0)
        varselProducer.finalize()
        varselProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `ikke send ut varsler før finalize blir kalt`() {
        varselProducer.avvikVurdert(false, 26.0)
        assertEquals(0, testRapid.inspektør.size)
        varselProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `produser riktig format på varsel`() {
        varselProducer.avvikVurdert(false, 26.0)
        varselProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
        val message = testRapid.inspektør.message(0)
        assertEquals("nye_varsler", message["@event_name"].asText())
        assertEquals(fødselsnummer.value, message["fødselsnummer"].asText())
        assertPresent(message["@id"])
        assertPresent(message["@opprettet"])
        assertEquals(1, message["aktiviteter"].size())
        val varsel = message["aktiviteter"].first()
        assertPresent(varsel)
        assertPresent(varsel["melding"])
        assertPresent(varsel["id"])
        assertPresent(varsel["tidsstempel"])
        assertEquals("VARSEL", varsel["nivå"].asText())
        assertEquals(1, varsel["kontekster"].size())
        val kontekst = varsel["kontekster"].first()
        assertEquals("Vedtaksperiode", kontekst["konteksttype"].asText())
        val kontekstMap = kontekst["kontekstmap"]
        assertEquals(vedtaksperiodeId.toString(), kontekstMap["vedtaksperiodeId"].asText())
    }

    @Test
    fun `produser varsel hvis avviket ikke er akseptabelt`() {
        varselProducer.avvikVurdert(false, 26.0)
        varselProducer.finalize()
        val message = testRapid.inspektør.message(0)
        val varsel = message["aktiviteter"][0]
        assertPresent(varsel)
        assertEquals("RV_IV_2", varsel["varselkode"].asText())
    }

    private fun assertPresent(jsonNode: JsonNode?) {
        assertNotNull(jsonNode) { "Forventer at noden ikke er null" }
        jsonNode?.isMissingOrNull()?.let { assertFalse(it) { "Forventer at noden ikke mangler" } }
    }
}