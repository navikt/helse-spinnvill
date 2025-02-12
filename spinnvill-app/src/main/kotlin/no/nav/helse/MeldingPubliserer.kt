package no.nav.helse

import no.nav.helse.avviksvurdering.Avviksvurdering
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.mediator.producer.Message
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.*

class MeldingPubliserer(private val rapidsConnection: RapidsConnection, private val avviksvurderingBehov: AvviksvurderingBehov) {

    private val meldinger = mutableListOf<Message>()

    fun sendMeldinger() {
        meldinger.map { message ->
            when (message) {
                is Message.Behov -> message to JsonMessage.newNeed(message.behov, message.innhold)
                is Message.Hendelse -> message to JsonMessage.newMessage(message.navn, message.innhold)
                is Message.Løsning -> message to JsonMessage.newMessage(message.innhold)
            }
        }.onEach { (_, json) ->
            json["fødselsnummer"] = avviksvurderingBehov.fødselsnummer.value
            json["organisasjonsnummer"] = avviksvurderingBehov.organisasjonsnummer
            json["skjæringstidspunkt"] = avviksvurderingBehov.skjæringstidspunkt
            json["vedtaksperiodeId"] = avviksvurderingBehov.vedtaksperiodeId
        }.forEach {
            rapidsConnection.publish(it.second.toJson())
        }
    }

    fun subsumsjon() {

    }

    fun behovløsningUtenVurdering(avviksvurderingId: UUID) {
        val løsningMap = avviksvurderingBehov.json.toMutableMap().apply {
            this["@løsning"] = mapOf(
                "Avviksvurdering" to mapOf(
                    "utfall" to "TrengerIkkeNyVurdering",
                    "avviksvurderingId" to avviksvurderingId
                )
            )
        }
        meldinger.add(Message.Løsning(løsningMap))
    }

    fun behovløsningMedVurdering(vurdering: Avviksvurdering) {
        val løsningMap = avviksvurderingBehov.json.toMutableMap().apply {
            this["@løsning"] = mapOf(
                "Avviksvurdering" to mapOf(
                    "utfall" to "NyVurderingForetatt",
                    "avviksvurderingId" to vurdering.id,
                    "avviksprosent" to vurdering.avviksprosent,
                    "harAkseptabeltAvvik" to vurdering.harAkseptabeltAvvik,
                    "maksimaltTillattAvvik" to vurdering.maksimaltTillattAvvik,
                    "opprettet" to vurdering.vurderingstidspunkt,
                    "beregningsgrunnlag" to mapOf(
                        "totalbeløp" to vurdering.beregningsgrunnlag.totalOmregnetÅrsinntekt,
                        "omregnedeÅrsinntekter" to vurdering.beregningsgrunnlag.omregnedeÅrsinntekter.map { (arbeidsgiverreferanse, beløp) ->
                            mapOf(
                                "arbeidsgiverreferanse" to arbeidsgiverreferanse,
                                "beløp" to beløp
                            )
                        }
                    ),
                    "sammenligningsgrunnlag" to mapOf(
                        "totalbeløp" to vurdering.sammenligningsgrunnlag.totaltInnrapportertÅrsinntekt,
                        "innrapporterteInntekter" to vurdering.sammenligningsgrunnlag.inntekter.map { (arbeidsgiverreferanse, inntekter) ->
                            mapOf(
                                "arbeidsgiverreferanse" to arbeidsgiverreferanse,
                                "inntekter" to inntekter.map { (årMåned, beløp) ->
                                    mapOf(
                                        "årMåned" to årMåned,
                                        "beløp" to beløp
                                    )
                                }
                            )
                        }
                    )
                )
            )
        }
        meldinger.add(Message.Løsning(løsningMap))
    }

    fun behovForSammenligningsgrunnlag(behov: BehovForSammenligningsgrunnlag) {
        val behovNavn = "InntekterForSammenligningsgrunnlag"
        meldinger.add(Message.Behov(setOf(behovNavn), mapOf(behovNavn to behov.toMap())))
    }

    private fun BehovForSammenligningsgrunnlag.toMap(): Map<String, Any> {
        return mapOf(
            "beregningStart" to beregningsperiodeFom,
            "beregningSlutt" to beregningsperiodeTom,
            "skjæringstidspunkt" to skjæringstidspunkt
        )
    }
}