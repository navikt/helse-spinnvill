package no.nav.helse.mediator.producer

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Fødselsnummer
import no.nav.helse.mediator.producer.AvviksvurderingProducer.Companion.toHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class MigrerteAvviksvurderingerProducer(
    private val fødselsnummer: Fødselsnummer,
    private val rapidsConnection: RapidsConnection
) {
    private val avviksvurderingerKø = mutableListOf<AvviksvurderingForKafka>()

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun nyAvviksvurdering(
        vilkårsgrunnlagId: UUID,
        skjæringstidspunkt: LocalDate,
        avviksvurderingDto: AvviksvurderingProducer.AvviksvurderingDto,
    ) {
        avviksvurderingerKø.add(
            AvviksvurderingForKafka(vilkårsgrunnlagId, skjæringstidspunkt, avviksvurderingDto)
        )
    }

    internal fun publiserMeldinger() {
        avviksvurderingerKø
            .map { avviksvurdering ->
                avviksvurdering.skjæringstidspunkt to avviksvurdering.avviksvurderingDto.toHendelse(avviksvurdering.vilkårsgrunnlagId).let {
                    JsonMessage.newMessage(it.navn, it.innhold)
                }
            }
            .onEach { (skjæringstidspunkt, json) ->
                json["fødselsnummer"] = fødselsnummer.value
                json["skjæringstidspunkt"] = skjæringstidspunkt
            }
            .forEach { (skjæringstidspunkt, jsonMessage) ->
                sikkerlogg.info(
                    "Publiserer migrert avviksvurdering for {}, {}",
                    kv("fødselsnummer", fødselsnummer),
                    kv("skjæringstidspunkt", skjæringstidspunkt)
                )
                rapidsConnection.publish(fødselsnummer.value, jsonMessage.toJson())
            }
    }

    private data class AvviksvurderingForKafka(
        val vilkårsgrunnlagId: UUID,
        val skjæringstidspunkt: LocalDate,
        val avviksvurderingDto: AvviksvurderingProducer.AvviksvurderingDto
    )
}