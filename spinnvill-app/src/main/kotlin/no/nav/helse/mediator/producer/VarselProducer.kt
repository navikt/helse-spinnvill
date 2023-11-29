package no.nav.helse.mediator.producer

import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.*

internal class VarselProducer(
    private val fødselsnummer: Fødselsnummer,
    private val vedtaksperiodeId: UUID,
    private val rapidsConnection: RapidsConnection
): KriterieObserver {
    private val varselkø = mutableListOf<VarselDto>()

    override fun avvikVurdert(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag
    ) {
        if (harAkseptabeltAvvik) return
        varselkø.add(VarselDto(
            melding = "Utenfor akseptabelt avvik. Avviket er $avviksprosent %.",
            varselkode = "RV_IV_2"
        ))
    }

    internal fun finalize() {
        if (varselkø.isEmpty()) return
        val message = JsonMessage.newMessage(
            "nye_varsler",
            mapOf(
                "fødselsnummer" to fødselsnummer.value,
                "aktiviteter" to varselkø.map { varsel ->
                    mapOf(
                        "melding" to varsel.melding,
                        "id" to varsel.id,
                        "tidsstempel" to varsel.tidsstempel,
                        "nivå" to "VARSEL",
                        "varselkode" to varsel.varselkode,
                        "kontekster" to listOf(
                            mapOf(
                                "konteksttype" to "Vedtaksperiode",
                                "kontekstmap" to mapOf(
                                    "vedtaksperiodeId" to vedtaksperiodeId
                                )
                            )
                        )
                    )
                }
            )
        )
        rapidsConnection.publish(fødselsnummer.value, message.toJson())
        varselkø.clear()
    }

    private data class VarselDto(
        val melding: String,
        val varselkode: String,
    ) {
        val id: UUID = UUID.randomUUID()
        val tidsstempel: LocalDateTime = LocalDateTime.now()
    }
}