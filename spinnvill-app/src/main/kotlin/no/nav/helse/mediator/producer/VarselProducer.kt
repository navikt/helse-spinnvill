package no.nav.helse.mediator.producer

import java.time.LocalDateTime
import java.util.*

internal class VarselProducer(
    private val vedtaksperiodeId: UUID
): Producer {
    private val varselkø = mutableListOf<VarselDto>()

    fun avvikVurdert(harAkseptabeltAvvik: Boolean, avviksprosent: Double) {
        if (harAkseptabeltAvvik) return
        varselkø.add(VarselDto(
            melding = "Utenfor akseptabelt avvik. Avviket er $avviksprosent %.",
            varselkode = "RV_IV_2"
        ))
    }

    override fun ferdigstill(): List<Message> {
        if (varselkø.isEmpty()) return emptyList()
        val message = Message.Hendelse(
            navn = "nye_varsler",
            innhold = mapOf(
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
        varselkø.clear()
        return listOf(message)
    }

    private data class VarselDto(
        val melding: String,
        val varselkode: String,
    ) {
        val id: UUID = UUID.randomUUID()
        val tidsstempel: LocalDateTime = LocalDateTime.now()
    }
}
