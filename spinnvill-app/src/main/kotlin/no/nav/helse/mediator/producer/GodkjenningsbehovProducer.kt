package no.nav.helse.mediator.producer

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.*
import no.nav.helse.kafka.GodkjenningsbehovMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class GodkjenningsbehovProducer(
    private val godkjenningsbehovMessage: GodkjenningsbehovMessage,
) : Producer {

    private val godkjenningsbehov = mutableListOf<GodkjenningsbehovMessage>()

    internal fun registrerGodkjenningsbehovForUtsending(avviksvurderingsgrunnlag: Avviksvurderingsgrunnlag) {
        avviksvurderingsgrunnlag.accept(Sammensyer(godkjenningsbehovMessage))
        godkjenningsbehov.add(godkjenningsbehovMessage)
    }

    override fun ferdigstill(): List<Message> =
        godkjenningsbehov.map { Message.Behov(setOf("Godkjenning"), it.utgående()) }

    private class Sammensyer(private val godkjenningsbehovMessage: GodkjenningsbehovMessage) : Visitor {
        override fun visitAvviksvurderingsgrunnlag(
            id: UUID,
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
            kilde: Kilde,
            opprettet: LocalDateTime,
            beregningsgrunnlag: IBeregningsgrunnlag,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
        ) {
            if (kilde == Kilde.INFOTRYGD) return
            godkjenningsbehovMessage.leggTilAvviksvurderingId(id)
        }
    }
}
