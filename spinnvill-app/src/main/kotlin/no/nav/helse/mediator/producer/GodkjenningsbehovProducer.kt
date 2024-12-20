package no.nav.helse.mediator.producer

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.Avviksvurdering
import no.nav.helse.avviksvurdering.Kilde
import no.nav.helse.avviksvurdering.Visitor
import no.nav.helse.kafka.FastsattISpleis
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class GodkjenningsbehovProducer(
    private val godkjenningsbehovMessage: FastsattISpleis,
) : Producer {

    private val godkjenningsbehov = mutableListOf<FastsattISpleis>()

    internal fun registrerGodkjenningsbehovForUtsending(avviksvurdering: Avviksvurdering) {
        avviksvurdering.accept(Sammensyer(godkjenningsbehovMessage))
        godkjenningsbehov.add(godkjenningsbehovMessage)
    }

    override fun ferdigstill(): List<Message> =
        godkjenningsbehov.map { Message.Behov(setOf("Godkjenning"), it.utgående()) }

    private class Sammensyer(private val godkjenningsbehovMessage: FastsattISpleis) : Visitor {
        override fun visitAvviksvurdering(
            id: UUID,
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
            kilde: Kilde,
            opprettet: LocalDateTime,
        ) {
            godkjenningsbehovMessage.leggTilAvviksvurderingId(id)
        }
    }
}
