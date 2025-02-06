package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.*

class AvviksvurderingbehovMessage(packet: JsonMessage) {
    val vilkårsgrunnlagId: UUID = packet["vilkårsgrunnlagId"].asUUID()
    val skjæringstidspunkt: LocalDate = packet["skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val vedtaksperiodeId: UUID = packet["vedtaksperiodeId"].asUUID()
    val organisasjonsnummer: String = packet["organisasjonsnummer"].asText()
    val beregningsgrunnlag = packet["omregnedeÅrsinntekter"].associate {
        it["organisasjonsnummer"].asText() to it["beløp"].asDouble()
    }
}
