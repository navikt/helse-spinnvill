package no.nav.helse.mediator.producer

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
import no.nav.helse.VersjonAvKode
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SubsumsjonProducer(
    private val fødselsnummer: Fødselsnummer,
    private val organisasjonsnummer: Arbeidsgiverreferanse,
    private val vedtaksperiodeId: UUID,
    private val vilkårsgrunnlagId: UUID,
    private val versjonAvKode: VersjonAvKode,
    private val rapidsConnection: RapidsConnection
) : KriterieObserver {
    private val subsumsjonskø = mutableListOf<SubsumsjonsmeldingDto>()

    override fun avvikVurdert(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double,
    ) {

        subsumsjonskø.add(
            AvviksvurderingSubsumsjonBuilder(
                harAkseptabeltAvvik = harAkseptabeltAvvik,
                avviksprosent = avviksprosent,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag
            ).build()
        )
    }

    internal fun finalize() {
        subsumsjonskø.forEach {
            rapidsConnection.publish(fødselsnummer.value, JsonMessage.newMessage(
                "subsumsjon",
                mapOf(
                    "subsumsjon" to mutableMapOf(
                        "fodselsnummer" to fødselsnummer.value,
                        "id" to it.id,
                        "tidsstempel" to it.tidsstempel,
                        "kilde" to "spinnvill",
                        "versjon" to "1.0.0",
                        "paragraf" to it.paragraf,
                        "lovverk" to it.lovverk,
                        "lovverksversjon" to it.lovverksversjon,
                        "utfall" to it.utfall,
                        "input" to it.input,
                        "output" to it.output,
                        "sporing" to mapOf(
                            "organisasjonsnummer" to listOf(organisasjonsnummer.value),
                            "vedtaksperiode" to listOf(vedtaksperiodeId.toString()),
                            "vilkårsgrunnlag" to listOf(vilkårsgrunnlagId.toString())
                        ),
                        "versjonAvKode" to versjonAvKode
                    ).apply {
                        compute("ledd") { _, _ -> it.ledd }
                        compute("bokstav") { _, _ -> it.bokstav }
                        compute("punktum") { _, _ -> it.punktum }
                    },
                )
            ).toJson())
        }
        subsumsjonskø.clear()
    }

    internal data class SubsumsjonsmeldingDto(
        val paragraf: String,
        val ledd: Int?,
        val bokstav: String?,
        val punktum: Int?,
        val lovverk: String,
        val lovverksversjon: LocalDate,
        val utfall: Utfall,
        val input: Map<String, Any>,
        val output: Map<String, Any>,
    ) {
        val id: UUID = UUID.randomUUID()
        val tidsstempel: LocalDateTime = LocalDateTime.now()
    }

    internal enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }
}