package no.nav.helse.mediator.producer

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class VarselProducer(
    private val fødselsnummer: Fødselsnummer,
    private val vedtaksperiodeId: UUID,
    private val rapidsConnection: RapidsConnection
): KriterieObserver {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val varselkø = mutableListOf<VarselDto>()

    override fun avvikVurdert(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double
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
        ).toJson()
        logg.info("Sender ut ${varselkø.size} varsler for {}",
            kv("vedtaksperiodeId", vedtaksperiodeId),
        )
        sikkerlogg.info("Sender ut ${varselkø.size} varsler for {}, {}. Varsler: {}",
            kv("fødselsnummer", fødselsnummer),
            kv("vedtaksperiodeId", vedtaksperiodeId),
            message
        )
        rapidsConnection.publish(fødselsnummer.value, message)
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