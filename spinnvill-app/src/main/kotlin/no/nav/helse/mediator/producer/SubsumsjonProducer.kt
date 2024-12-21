package no.nav.helse.mediator.producer

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.VersjonAvKode
import no.nav.helse.avviksvurdering.Avviksvurdering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SubsumsjonProducer(
    private val fødselsnummer: Fødselsnummer,
    private val organisasjonsnummer: Arbeidsgiverreferanse,
    private val vedtaksperiodeId: UUID,
    private val vilkårsgrunnlagId: UUID,
    private val versjonAvKode: VersjonAvKode,
) : Producer {

    private val subsumsjonskø = mutableListOf<SubsumsjonsmeldingDto>()

    fun avvikVurdert(vurdering: Avviksvurdering) {
        val builder = AvviksvurderingSubsumsjonBuilder(
            id = vurdering.id,
            harAkseptabeltAvvik = vurdering.harAkseptabeltAvvik,
            avviksprosent = vurdering.avviksprosent,
            maksimaltTillattAvvik = vurdering.maksimaltTillattAvvik,
            beregningsgrunnlag = vurdering.beregningsgrunnlag,
            sammenligningsgrunnlag = vurdering.sammenligningsgrunnlag
        )
        subsumsjonskø.add(builder.`8-30 ledd 2 punktum 1`())

        if (vurdering.harAkseptabeltAvvik) subsumsjonskø.add(builder.`8-30 ledd 1`())
    }

    override fun ferdigstill(): List<Message> {
        if (subsumsjonskø.isEmpty()) return emptyList()
        val meldinger = subsumsjonskø.map {
            Message.Hendelse(
                navn = "subsumsjon",
                innhold = mapOf(
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
            )
        }
        subsumsjonskø.clear()
        return meldinger
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
