package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.*
import no.nav.helse.avviksvurdering.*
import no.nav.helse.db.Database
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.kafka.*
import no.nav.helse.mediator.producer.*
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class Mediator(
    private val versjonAvKode: VersjonAvKode,
    private val rapidsConnection: RapidsConnection,
    private val database: Database
) : MessageHandler {

    init {
        UtkastTilVedtakRiver(rapidsConnection, this)
        SammenligningsgrunnlagRiver(rapidsConnection, this)
    }

    override fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage) {
        logg.info("Behandler utkast_til_vedtak for {}", kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId))
        sikkerlogg.info(
            "Behandler utkast_til_vedtak for {}, {}",
            kv("fødselsnummer", utkastTilVedtakMessage.fødselsnummer),
            kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId)
        )
        val meldingProducer = nyMeldingProducer(utkastTilVedtakMessage)
        val behovProducer = BehovProducer(utkastTilVedtakJson = utkastTilVedtakMessage.toJson())
        val varselProducer = VarselProducer(vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId)
        val subsumsjonProducer = nySubsumsjonProducer(utkastTilVedtakMessage)
        val avviksvurderingProducer = AvviksvurderingProducer(vilkårsgrunnlagId = utkastTilVedtakMessage.vilkårsgrunnlagId)
        val utkastTilVedtakProducer = UtkastTilVedtakProducer(utkastTilVedtakMessage)
        meldingProducer.nyProducer(behovProducer, varselProducer, subsumsjonProducer, avviksvurderingProducer, utkastTilVedtakProducer)
        val beregningsgrunnlag = nyttBeregningsgrunnlag(utkastTilVedtakMessage)
        val avviksvurdering = finnAvviksvurdering(
            Fødselsnummer(utkastTilVedtakMessage.fødselsnummer),
            utkastTilVedtakMessage.skjæringstidspunkt
        )
            ?.vurderBehovForNyVurdering(beregningsgrunnlag)

        if (avviksvurdering == null) {
            logg.info("Trenger sammenligningsgrunnlag, {}", kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId))
            beOmSammenligningsgrunnlag(utkastTilVedtakMessage.skjæringstidspunkt, behovProducer)
        } else {
            logg.info("Har sammenligningsgrunnlag, starter avviksvurdering, {}", kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId))
            håndter(
                beregningsgrunnlag = beregningsgrunnlag,
                varselProducer = varselProducer,
                subsumsjonProducer = subsumsjonProducer,
                avviksvurderingProducer = avviksvurderingProducer,
                utkastTilVedtakProducer = utkastTilVedtakProducer,
                avviksvurdering = avviksvurdering,
            )
            logg.info("Avviksvurdering utført, {}", kv("vedtaksperiodeId", utkastTilVedtakMessage.vedtaksperiodeId))
        }
        meldingProducer.publiserMeldinger()
    }

    override fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage) {
        val fødselsnummer = Fødselsnummer(sammenligningsgrunnlagMessage.fødselsnummer)
        val skjæringstidspunkt = sammenligningsgrunnlagMessage.skjæringstidspunkt

        if (finnAvviksvurdering(fødselsnummer, skjæringstidspunkt) != null) {
            logg.warn("Ignorerer duplikat sammenligningsgrunnlag for eksisterende avviksvurdering")
            sikkerlogg.warn(
                "Ignorerer duplikat sammenligningsgrunnlag for {} {}",
                kv("fødselsnummer", fødselsnummer.value),
                kv("skjæringstidspunkg", skjæringstidspunkt)
            )
            return
        }

        database.lagreAvviksvurdering(
            AvviksvurderingDto(
                id = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                sammenligningsgrunnlag = sammenligningsgrunnlagMessage.sammenligningsgrunnlag.dto(),
                beregningsgrunnlag = null
            )
        )

        rapidsConnection.queueReplayMessage(fødselsnummer.value, sammenligningsgrunnlagMessage.utkastTilVedtakJson)
    }

    private fun håndter(
        beregningsgrunnlag: Beregningsgrunnlag,
        varselProducer: VarselProducer,
        subsumsjonProducer: SubsumsjonProducer,
        avviksvurderingProducer: AvviksvurderingProducer,
        utkastTilVedtakProducer: UtkastTilVedtakProducer,
        avviksvurdering: Avviksvurdering,
    ) {
        avviksvurdering.register(varselProducer)
        avviksvurdering.register(subsumsjonProducer)
        avviksvurdering.register(avviksvurderingProducer)
        avviksvurdering.håndter(beregningsgrunnlag)
        utkastTilVedtakProducer.registrerUtkastForUtsending(avviksvurdering)
        val builder = DatabaseDtoBuilder()
        avviksvurdering.accept(builder)
        database.lagreAvviksvurdering(builder.build())
    }

    private fun finnAvviksvurdering(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurdering? {
        return database.finnSisteAvviksvurdering(fødselsnummer, skjæringstidspunkt)?.tilDomene()
    }

    private fun beOmSammenligningsgrunnlag(skjæringstidspunkt: LocalDate, behovProducer: BehovProducer) {
        val tom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        val fom = tom.minusMonths(11)
        behovProducer.sammenligningsgrunnlag(
            BehovForSammenligningsgrunnlag(
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsperiodeFom = fom,
                beregningsperiodeTom = tom
            )
        )
    }

    private fun nyMeldingProducer(utkastTilVedtakMessage: UtkastTilVedtakMessage) = MeldingProducer(
        aktørId = utkastTilVedtakMessage.aktørId.somAktørId(),
        fødselsnummer = utkastTilVedtakMessage.fødselsnummer.somFnr(),
        vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId,
        organisasjonsnummer = utkastTilVedtakMessage.organisasjonsnummer.somArbeidsgiverref(),
        skjæringstidspunkt = utkastTilVedtakMessage.skjæringstidspunkt,
        rapidsConnection = rapidsConnection
    )

    private fun nySubsumsjonProducer(utkastTilVedtakMessage: UtkastTilVedtakMessage): SubsumsjonProducer {
        return SubsumsjonProducer(
            fødselsnummer = utkastTilVedtakMessage.fødselsnummer.somFnr(),
            vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId,
            organisasjonsnummer = utkastTilVedtakMessage.organisasjonsnummer.somArbeidsgiverref(),
            vilkårsgrunnlagId = utkastTilVedtakMessage.vilkårsgrunnlagId,
            versjonAvKode = versjonAvKode
        )
    }

    private fun nyttBeregningsgrunnlag(utkastTilVedtakMessage: UtkastTilVedtakMessage): Beregningsgrunnlag {
        return Beregningsgrunnlag.opprett(
            utkastTilVedtakMessage.beregningsgrunnlag.entries.associate {
                Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value)
            }
        )
    }

    internal companion object {
        private val logg = LoggerFactory.getLogger(Mediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")


        internal fun AvviksvurderingDto.tilDomene(): Avviksvurdering {
            val beregningsgrunnlag = beregningsgrunnlag?.let {
                Beregningsgrunnlag.opprett(it.omregnedeÅrsinntekter)
            } ?: Beregningsgrunnlag.INGEN

            return Avviksvurdering(
                id = id,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = Sammenligningsgrunnlag(
                    sammenligningsgrunnlag.innrapporterteInntekter.map { (organisasjonsnummer, inntekter) ->
                        ArbeidsgiverInntekt(
                            arbeidsgiverreferanse = organisasjonsnummer,
                            inntekter = inntekter.map {
                                ArbeidsgiverInntekt.MånedligInntekt(
                                    inntekt = it.inntekt,
                                    måned = it.måned,
                                    fordel = it.fordel,
                                    beskrivelse = it.beskrivelse,
                                    inntektstype = it.inntektstype.tilDomene()
                                )
                            }
                        )
                    }
                )
            )
        }

        private fun Map<String, List<SammenligningsgrunnlagMessage.Inntekt>>.dto(): AvviksvurderingDto.SammenligningsgrunnlagDto =
            AvviksvurderingDto.SammenligningsgrunnlagDto(
                this.entries.associate { (organisasjonsnummer, inntekter) ->
                    Arbeidsgiverreferanse(organisasjonsnummer) to inntekter.map {
                        AvviksvurderingDto.MånedligInntektDto(
                            InntektPerMåned(it.beløp),
                            it.årMåned,
                            it.fordel,
                            it.beskrivelse,
                            it.inntektstype.tilDto()
                        )
                    }
                }
            )

        private fun SammenligningsgrunnlagMessage.Inntektstype.tilDto(): AvviksvurderingDto.InntektstypeDto {
            return when (this) {
                SammenligningsgrunnlagMessage.Inntektstype.LØNNSINNTEKT -> AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT
                SammenligningsgrunnlagMessage.Inntektstype.NÆRINGSINNTEKT -> AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT
                SammenligningsgrunnlagMessage.Inntektstype.PENSJON_ELLER_TRYGD -> AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD
                SammenligningsgrunnlagMessage.Inntektstype.YTELSE_FRA_OFFENTLIGE -> AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE
            }
        }

        private fun AvviksvurderingDto.InntektstypeDto.tilDomene(): ArbeidsgiverInntekt.Inntektstype {
            return when (this) {
                AvviksvurderingDto.InntektstypeDto.LØNNSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                AvviksvurderingDto.InntektstypeDto.NÆRINGSINNTEKT -> ArbeidsgiverInntekt.Inntektstype.NÆRINGSINNTEKT
                AvviksvurderingDto.InntektstypeDto.PENSJON_ELLER_TRYGD -> ArbeidsgiverInntekt.Inntektstype.PENSJON_ELLER_TRYGD
                AvviksvurderingDto.InntektstypeDto.YTELSE_FRA_OFFENTLIGE -> ArbeidsgiverInntekt.Inntektstype.YTELSE_FRA_OFFENTLIGE
            }
        }
    }
}
