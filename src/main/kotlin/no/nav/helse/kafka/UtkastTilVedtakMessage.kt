package no.nav.helse.kafka

import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

class UtkastTilVedtakMessage(packet: JsonMessage) {

    val skjæringstidspunkt: LocalDate = packet["skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val beregningsgrunnlag = packet["omregnedeÅrsinntekter"].map {
        it["organisasjonsnummer"].asText() to it["beløp"].asDouble()
    }.let {
        Beregningsgrunnlag(it.toMap())
    }
}
