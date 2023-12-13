package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.*

class UtkastTilVedtakMessage(private val packet: JsonMessage) {
    fun toJson(): JsonNode {
        return objectMapper.readTree(packet.toJson())
    }

    fun finalize(): Map<String, Any> =
        objectMapper.readValue<Map<String, Any>>(packet.apply {
            packet["behandlingStartet"] = true
            packet["avviksvurderingId"] = UUID.randomUUID()
        }.toJson())

    val vilkårsgrunnlagId: UUID = packet["Godkjenning.vilkårsgrunnlagId"].asUUID()
    val skjæringstidspunkt: LocalDate = packet["Godkjenning.skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val aktørId: String = packet["aktørId"].asText()
    val vedtaksperiodeId: UUID = packet["vedtaksperiodeId"].asUUID()
    val organisasjonsnummer: String = packet["organisasjonsnummer"].asText()
    val beregningsgrunnlag = packet["Godkjenning.omregnedeÅrsinntekter"].associate {
        it["organisasjonsnummer"].asText() to it["beløp"].asDouble()
    }
}
