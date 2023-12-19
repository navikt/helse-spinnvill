package no.nav.helse.mediator.producer

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.InntektPerMåned
import no.nav.helse.KriterieObserver
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AvviksvurderingProducer(private val vilkårsgrunnlagId: UUID) : KriterieObserver, Producer {
    private val avviksvurderingKø = mutableListOf<AvviksvurderingDto>()
    override fun avvikVurdert(
        id: UUID,
        harAkseptabeltAvvik: Boolean,
        avviksprosent: Double,
        beregningsgrunnlag: Beregningsgrunnlag,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
        maksimaltTillattAvvik: Double
    ) {
        avviksvurderingKø.add(
            AvviksvurderingSubsumsjonBuilder(
                id = id,
                harAkseptabeltAvvik = harAkseptabeltAvvik,
                avviksprosent = avviksprosent,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag
            ).buildAvviksvurdering()
        )
    }

    override fun ferdigstill(): List<Message> {
        if (avviksvurderingKø.isEmpty()) return emptyList()
        val meldinger = avviksvurderingKø.map { dto -> dto.toHendelse(vilkårsgrunnlagId) }
        avviksvurderingKø.clear()
        return meldinger
    }

    internal data class AvviksvurderingDto(
        val id: UUID,
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

    internal companion object {
        internal fun AvviksvurderingDto.toHendelse(vilkårsgrunnlagId: UUID): Message.Hendelse {
            return Message.Hendelse(
                navn = "avvik_vurdert",
                innhold = mapOf(
                    "avviksvurdering" to mapOf(
                        "id" to id,
                        "vilkårsgrunnlagId" to vilkårsgrunnlagId,
                        "opprettet" to LocalDateTime.now(),
                        "avviksprosent" to avviksprosent,
                        "beregningsgrunnlag" to mapOf(
                            "totalbeløp" to beregningsgrunnlagTotalbeløp,
                            "omregnedeÅrsinntekter" to omregnedeÅrsinntekter.map { (arbeidsgiverreferanse, beløp) ->
                                mapOf(
                                    "arbeidsgiverreferanse" to arbeidsgiverreferanse,
                                    "beløp" to beløp
                                )
                            }
                        ),
                        "sammenligningsgrunnlag" to mapOf(
                            "totalbeløp" to sammenligningsgrunnlagTotalbeløp,
                            "innrapporterteInntekter" to innrapporterteInntekter.map { (arbeidsgiverreferanse, inntekter) ->
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
            )
        }
    }
}
