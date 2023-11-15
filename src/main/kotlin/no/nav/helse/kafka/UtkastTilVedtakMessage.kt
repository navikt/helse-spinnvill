package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

class UtkastTilVedtakMessage(packet: JsonMessage) {

    val skjæringstidspunkt: LocalDate = packet["Godkjenning.skjæringstidspunkt"].asLocalDate()
    val fødselsnummer: String = packet["fødselsnummer"].asText()
    val beregningsgrunnlag = packet["Godkjenning.omregnedeÅrsinntekter"].associate {
        it["organisasjonsnummer"].asText() to it["beløp"].asDouble()
    }
}
