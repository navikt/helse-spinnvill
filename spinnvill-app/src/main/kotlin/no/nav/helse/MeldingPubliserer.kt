package no.nav.helse

import no.nav.helse.avviksvurdering.Avviksvurdering
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.BehovForSammenligningsgrunnlag
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class MeldingPubliserer(
    private val rapidsConnection: RapidsConnection,
    private val avviksvurderingBehov: AvviksvurderingBehov,
    private val versjonAvKode: VersjonAvKode,
) {

    private val meldinger = mutableListOf<Melding>()

    fun sendMeldinger() {
        meldinger.map { message ->
            when (message) {
                is Melding.Behov -> message to JsonMessage.newNeed(message.behov, message.innhold)
                is Melding.Hendelse -> message to JsonMessage.newMessage(message.navn, message.innhold)
                is Melding.Løsning -> message to JsonMessage.newMessage(message.innhold)
            }
        }.onEach { (_, json) ->
            json["fødselsnummer"] = avviksvurderingBehov.fødselsnummer.value
            json["organisasjonsnummer"] = avviksvurderingBehov.organisasjonsnummer
            json["skjæringstidspunkt"] = avviksvurderingBehov.skjæringstidspunkt
            json["vedtaksperiodeId"] = avviksvurderingBehov.vedtaksperiodeId
        }.forEach {
            rapidsConnection.publish(avviksvurderingBehov.fødselsnummer.value, it.second.toJson())
        }
    }

    private fun SubsumsjonsmeldingDto.tilMelding(): Melding.Hendelse {
        val dto = this
        return Melding.Hendelse(
            navn = "subsumsjon",
            innhold = mapOf(
                "subsumsjon" to buildMap {
                    put("fodselsnummer", avviksvurderingBehov.fødselsnummer.value)
                    put("id", dto.id)
                    put("tidsstempel", dto.tidsstempel)
                    put("kilde", "spinnvill")
                    put("versjon", "1.0.0")
                    put("paragraf", dto.paragraf)
                    put("lovverk", dto.lovverk)
                    put("lovverksversjon", dto.lovverksversjon)
                    put("utfall", dto.utfall)
                    put("input", dto.input)
                    put("output", dto.output)
                    put(
                        "sporing",
                        mapOf(
                            "organisasjonsnummer" to listOf(avviksvurderingBehov.organisasjonsnummer),
                            "vedtaksperiode" to listOf(avviksvurderingBehov.vedtaksperiodeId.toString()),
                            "vilkårsgrunnlag" to listOf(avviksvurderingBehov.vilkårsgrunnlagId.toString())
                        ),
                    )
                    put("versjonAvKode", versjonAvKode.value)
                    if (dto.ledd != null) put("ledd", dto.ledd)
                    if (dto.bokstav != null) put("bokstav", dto.bokstav)
                    if (dto.punktum != null) put("punktum", dto.punktum)
                }
            )
        )
    }

    fun behovløsningUtenVurdering(vurdering: Avviksvurdering) {
        val løsningMap = avviksvurderingBehov.json.toMutableMap().apply {
            this["@løsning"] = mapOf(
                "Avviksvurdering" to buildMap {
                        put("utfall", "TrengerIkkeNyVurdering")
                        putAll(behovløsning(vurdering))
                },
            )
        }
        meldinger.add(Melding.Løsning(løsningMap))
    }

    fun behovløsningMedVurdering(vurdering: Avviksvurdering) {
        val løsningMap = avviksvurderingBehov.json.toMutableMap().apply {
            this["@løsning"] = mapOf(
                "Avviksvurdering" to buildMap {
                    put("utfall", "NyVurderingForetatt")
                    putAll(behovløsning(vurdering))
                }
            )
        }
        meldinger.add(Melding.Løsning(løsningMap))
    }

    private fun behovløsning(vurdering: Avviksvurdering): Map<String, Any> {
        return mapOf(
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
                        "inntekter" to inntekter.map { (beløp, årMåned) ->
                            mapOf(
                                "årMåned" to årMåned,
                                "beløp" to beløp
                            )
                        }
                    )
                }
            )
        )
    }

    fun behovForSammenligningsgrunnlag(behov: BehovForSammenligningsgrunnlag) {
        val behovNavn = "InntekterForSammenligningsgrunnlag"
        meldinger.add(
            Melding.Behov(
                setOf(behovNavn),
                mapOf(
                    behovNavn to (mapOf("avviksvurderingBehovId" to avviksvurderingBehov.behovId) + behov.toMap())
                )
            )
        )
    }

    private fun BehovForSammenligningsgrunnlag.toMap(): Map<String, Any> {
        return mapOf(
            "beregningStart" to beregningsperiodeFom,
            "beregningSlutt" to beregningsperiodeTom,
            "skjæringstidspunkt" to skjæringstidspunkt
        )
    }

    internal fun `8-30 ledd 1`(beregningsgrunnlag: Beregningsgrunnlag) {
        val subsumsjon = SubsumsjonsmeldingDto(
            paragraf = "8-30",
            ledd = 1,
            bokstav = null,
            punktum = null,
            lovverk = "folketrygdloven",
            lovverksversjon = LocalDate.of(2019, 1, 1),
            utfall = Utfall.VILKAR_BEREGNET,
            input = mapOf(
                "omregnedeÅrsinntekter" to beregningsgrunnlag.omregnedeÅrsinntekter.map { (arbeidsgiverreferanse, omregnetÅrsinntekt) ->
                    mapOf(
                        "arbeidsgiverreferanse" to arbeidsgiverreferanse.value,
                        "inntekt" to omregnetÅrsinntekt.value
                    )
                },
            ),
            output = mapOf(
                "grunnlagForSykepengegrunnlag" to beregningsgrunnlag.totalOmregnetÅrsinntekt,
            )
        )
        meldinger.add(subsumsjon.tilMelding())
    }

    internal fun `8-30 ledd 2 punktum 1`(avviksvurdering: Avviksvurdering) {
        val beregningsgrunnlag = avviksvurdering.beregningsgrunnlag
        val sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag
        val subsumsjon = SubsumsjonsmeldingDto(
            paragraf = "8-30",
            ledd = 2,
            bokstav = null,
            punktum = 1,
            lovverk = "folketrygdloven",
            lovverksversjon = LocalDate.of(2019, 1, 1),
            utfall = Utfall.VILKAR_BEREGNET,
            input = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to avviksvurdering.maksimaltTillattAvvik,
                "grunnlagForSykepengegrunnlag" to mapOf(
                    "totalbeløp" to beregningsgrunnlag.totalOmregnetÅrsinntekt,
                    "omregnedeÅrsinntekter" to beregningsgrunnlag.omregnedeÅrsinntekter.map { (arbeidsgiverreferanse, omregnetÅrsinntekt) ->
                        mapOf(
                            "arbeidsgiverreferanse" to arbeidsgiverreferanse.value,
                            "inntekt" to omregnetÅrsinntekt.value
                        )
                    }
                ),
                "sammenligningsgrunnlag" to mapOf(
                    "totalbeløp" to sammenligningsgrunnlag.totaltInnrapportertÅrsinntekt,
                    "innrapporterteMånedsinntekter" to sammenligningsgrunnlag.inntekter.flatMap { (arbeidsgiverreferanse, inntekter) ->
                        inntekter.map {
                            arbeidsgiverreferanse to it

                        }
                    }.groupBy { (_, inntekt) -> inntekt.måned }.map { (måned, inntekter) ->
                        mapOf(
                            "måned" to måned,
                            "inntekter" to inntekter.map { (arbeidsgiverreferanse, inntekt) ->
                                mapOf(
                                    "arbeidsgiverreferanse" to arbeidsgiverreferanse.value,
                                    "inntekt" to inntekt.inntekt.value,
                                    "fordel" to inntekt.fordel?.value,
                                    "beskrivelse" to inntekt.beskrivelse?.value,
                                    "inntektstype" to inntekt.inntektstype,
                                )
                            }
                        )
                    }
                )
            ),
            output = mapOf(
                "avviksprosent" to avviksvurdering.avviksprosent,
                "harAkseptabeltAvvik" to avviksvurdering.harAkseptabeltAvvik
            )
        )
        meldinger.add(subsumsjon.tilMelding())
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
