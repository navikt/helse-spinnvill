package no.nav.helse.mediator.producer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.helpers.dummyBeregningsgrunnlag
import no.nav.helse.helpers.dummySammenligningsgrunnlag
import no.nav.helse.helpers.toJson
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class VarselProducerTest {
    private val vedtaksperiodeId = UUID.randomUUID()
    private val varselProducer = VarselProducer(vedtaksperiodeId)

    @Test
    fun `ikke produser varsel hvis avviket er akseptabelt`() {
        varselProducer.avvikVurdert(UUID.randomUUID(), true, 20.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        varselProducer.ferdigstill()
        assertEquals(0, varselProducer.ferdigstill().size)
    }

    @Test
    fun `varselkø tømmes etter hver finalize`() {
        varselProducer.avvikVurdert(UUID.randomUUID(), false, 26.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        assertEquals(1, varselProducer.ferdigstill().size)
        assertEquals(0, varselProducer.ferdigstill().size)
    }

    @Test
    fun `produser riktig format på varsel`() {
        varselProducer.avvikVurdert(UUID.randomUUID(), false, 26.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        val messages = varselProducer.ferdigstill()
        assertEquals(1, messages.size)
        val message = messages[0]
        check(message is Message.Hendelse)
        assertEquals("nye_varsler", message.navn)
        val json = message.innhold.toJson()
        assertEquals(1, json["aktiviteter"].size())
        val varsel = json["aktiviteter"].first()
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
        varselProducer.avvikVurdert(UUID.randomUUID(), false, 26.0, dummyBeregningsgrunnlag, dummySammenligningsgrunnlag, 25.0)
        val messages = varselProducer.ferdigstill()
        val json = messages[0].innhold.toJson()
        val varsel = json["aktiviteter"][0]
        assertPresent(varsel)
        assertEquals("RV_IV_2", varsel["varselkode"].asText())
    }

    private fun assertPresent(jsonNode: JsonNode?) {
        assertNotNull(jsonNode) { "Forventer at noden ikke er null" }
        jsonNode?.isMissingOrNull()?.let { assertFalse(it) { "Forventer at noden ikke mangler" } }
    }
}