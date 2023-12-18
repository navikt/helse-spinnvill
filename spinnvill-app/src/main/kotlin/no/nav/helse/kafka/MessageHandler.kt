package no.nav.helse.kafka

interface MessageHandler {
    fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage)

    fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage)

    fun håndter(avviksvurderingerFraSpleisMessage: AvviksvurderingerFraSpleisMessage)
}