package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Fødselsnummer
import no.nav.helse.InntektPerMåned
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.avviksvurdering.*
import no.nav.helse.db.Database
import no.nav.helse.dto.AvviksvurderingDto
import no.nav.helse.kafka.*
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class Mediator(private val rapidsConnection: RapidsConnection, private val database: Database) : MessageHandler {

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

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
        val behovProducer = BehovProducer(
            aktørId = utkastTilVedtakMessage.aktørId,
            fødselsnummer = utkastTilVedtakMessage.fødselsnummer,
            vedtaksperiodeId = utkastTilVedtakMessage.vedtaksperiodeId,
            organisasjonsnummer = utkastTilVedtakMessage.organisasjonsnummer,
            rapidsConnection = rapidsConnection
        )
        val beregningsgrunnlag = Beregningsgrunnlag.opprett(utkastTilVedtakMessage.beregningsgrunnlag.entries.associate { Organisasjonsnummer(it.key) to OmregnetÅrsinntekt(it.value) })
        håndter(
            beregningsgrunnlag = beregningsgrunnlag,
            fødselsnummer = Fødselsnummer(utkastTilVedtakMessage.fødselsnummer),
            skjæringstidspunkt = utkastTilVedtakMessage.skjæringstidspunkt,
            behovProducer = behovProducer
        )
        behovProducer.finalize()
    }

    override fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage) {
        database.lagreAvviksvurdering(AvviksvurderingDto(
            id = UUID.randomUUID(),
            fødselsnummer = Fødselsnummer(sammenligningsgrunnlagMessage.fødselsnummer),
            skjæringstidspunkt = sammenligningsgrunnlagMessage.skjæringstidspunkt,
            sammenligningsgrunnlag = sammenligningsgrunnlagMessage.sammenligningsgrunnlag.dto(),
            beregningsgrunnlag = null
        ))
    }

    private fun håndter(
        beregningsgrunnlag: Beregningsgrunnlag,
        fødselsnummer: Fødselsnummer,
        skjæringstidspunkt: LocalDate,
        behovProducer: BehovProducer
    ) {
        val avviksvurdering = avviksvurdering(fødselsnummer, skjæringstidspunkt)
            ?: return beOmSammenligningsgrunnlag(skjæringstidspunkt, behovProducer)
        avviksvurdering.håndter(beregningsgrunnlag)
    }

    private fun avviksvurdering(fødselsnummer: Fødselsnummer, skjæringstidspunkt: LocalDate): Avviksvurdering? {
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

    private fun AvviksvurderingDto.tilDomene(): Avviksvurdering {
        val beregningsgrunnlag = this.beregningsgrunnlag?.let {
            Beregningsgrunnlag.opprett(it.omregnedeÅrsinntekter)
        } ?: Beregningsgrunnlag.INGEN

        return Avviksvurdering(
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(
                this.sammenligningsgrunnlag.innrapporterteInntekter.map { (organisasjonsnummer, inntekter) ->
                    ArbeidsgiverInntekt(
                        organisasjonsummer = organisasjonsnummer,
                        inntekter = inntekter.associate { it.måned to it.inntekt }
                    )
                }
            )
        )
    }

    private fun Map<String, List<SammenligningsgrunnlagMessage.Inntekt>>.dto(): AvviksvurderingDto.SammenligningsgrunnlagDto =
        AvviksvurderingDto.SammenligningsgrunnlagDto(
            this.entries.associate { (organisasjonsnummer, inntekter) ->
                Organisasjonsnummer(organisasjonsnummer) to inntekter.map {
                    AvviksvurderingDto.MånedligInntektDto(InntektPerMåned(it.beløp), it.årMåned)
                }
            }
        )
}