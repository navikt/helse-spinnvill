package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class FastsattIInfotrygdRiver(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.rejectKey("@løsning")
                it.rejectKey("behandletAvSpinnvill")
                it.requireKey("fødselsnummer")
                it.demandValue("Godkjenning.sykepengegrunnlagsfakta.fastsatt", "IInfotrygd")
            }
        }.register(this)
    }

    //Vi bryr oss ikke godkjenningsbehov der sykepengegrunnlaget er fastsatt i infotrygd.
    //Vi republiserer derfor godkjenningsbehovet umiddelbart i disse tilfellene.
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fastsattIInfotrygd = FastsattIInfotrygd(packet)
        val behov = JsonMessage.newNeed(listOf("Godkjenning"), fastsattIInfotrygd.utgående())
        context.publish(behov.toJson())
    }
}