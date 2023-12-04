package no.nav.helse.mediator.producer

import no.nav.helse.*
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AvviksvurderingProducer(
    private val fødselsnummer: Fødselsnummer,
    private val rapidsConnection: RapidsConnection,
    private val aktørId: AktørId,
    private val skjæringstidspunkt: LocalDate
) : KriterieObserver {
    private val avviksvurderingKø = mutableListOf<AvviksvurderingDto>()
    override fun avvikVurdert(
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double
    ) {
        avviksvurderingKø.add(
            AvviksvurderingSubsumsjonBuilder(
                harAkseptabeltAvvik,
                avviksprosent,
                maksimaltTillattAvvik,
                beregningsgrunnlag,
                sammenligningsgrunnlag
            ).buildAvviksvurdering()
        )
    }

    fun finalize() {
        if (avviksvurderingKø.isEmpty()) return
        avviksvurderingKø.forEach {
            rapidsConnection.publish(
                fødselsnummer.value,
                JsonMessage.newMessage(
                    "avviksvurdering",
                    mapOf(
                        "fødselsnummer" to fødselsnummer,
                        "aktørId" to aktørId,
                        "skjæringstidspunkt" to skjæringstidspunkt,
                        "avviksvurdering" to mapOf(
                            "opprettet" to LocalDateTime.now(),
                            "avviksprosent" to it.avviksprosent,
                            "beregningsgrunnlag" to mapOf(
                                "totalbeløp" to it.beregningsgrunnlagTotalbeløp,
                                "omregnedeÅrsinntekter" to it.omregnedeÅrsinntekter.map { (arbeidsgiverreferanse, beløp) ->
                                    mapOf(
                                        "arbeidsgiverreferanse" to arbeidsgiverreferanse,
                                        "beløp" to beløp
                                    )
                                }
                            ),
                            "sammenligningsgrunnlag" to mapOf(
                                "totalbeløp" to it.sammenligningsgrunnlagTotalbeløp,
                                "innrapporterteInntekter" to it.innrapporterteInntekter.map { (arbeidsgiverreferanse, inntekter) ->
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
                ).toJson()
            )
        }
        avviksvurderingKø.clear()
    }

    internal data class AvviksvurderingDto(
        val avviksprosent: Double,
        val beregningsgrunnlagTotalbeløp: Double,
        val sammenligningsgrunnlagTotalbeløp: Double,
        val omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>,
        val innrapporterteInntekter: List<InnrapportertInntektDto>
    ) {
        data class InnrapportertInntektDto(
            val arbeidsgiverreferanse: Arbeidsgiverreferanse,
            val inntekter: List<MånedligInntektDto>
        )

        data class MånedligInntektDto(
            val måned: YearMonth,
            val beløp: InntektPerMåned,
        )
    }
}
