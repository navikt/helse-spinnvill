package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.*

class FastsattIInfotrygd(packet: JsonMessage) : GodkjenningsbehovMessage(packet) {

}

class FastsattISpleis(packet: JsonMessage) : GodkjenningsbehovMessage(packet) {
    private lateinit var avviksvurderingId: UUID
    fun leggTilAvviksvurderingId(id: UUID) {
        avviksvurderingId = id
    }

    override fun felterForUtgåendeMelding(): Map<String, Any> {
        return mapOf(
            "avviksvurderingId" to avviksvurderingId
        )
    }

    fun toJson(): JsonNode = result.deepCopy()

    val vilkårsgrunnlagId: UUID = packet["Godkjenning.vilkårsgrunnlagId"].asUUID()
    val skjæringstidspunkt: LocalDate = packet["Godkjenning.skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val vedtaksperiodeId: UUID = packet["vedtaksperiodeId"].asUUID()
    val organisasjonsnummer: String = packet["organisasjonsnummer"].asText()
    val beregningsgrunnlag = packet["Godkjenning.omregnedeÅrsinntekter"].associate {
        it["organisasjonsnummer"].asText() to it["beløp"].asDouble()
    }
}

abstract class GodkjenningsbehovMessage(packet: JsonMessage) {

    // Litt usikker på om vi trenger denne, det føltes bare litt feil å fikle på det objektet vi får inn i konstruktøren
    protected val result = objectMapper.readTree(packet.toJson()) as ObjectNode


    fun utgående(): Map<String, Any> =
        objectMapper.convertValue<Map<String, Any>>(result) + felterForUtgåendeMelding() + mapOf("behandletAvSpinnvill" to  true)

    open fun felterForUtgåendeMelding(): Map<String, Any> = emptyMap()

}
