package no.nav.helse.kafka

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksvurderingerFraSpleisRiverTest {

    private val testRapid = TestRapid()

    private val messageHandler = object: MessageHandler {
        var håndterteAvviksvurderinger = 0
            private set
        override fun håndter(utkastTilVedtakMessage: UtkastTilVedtakMessage) = TODO("Not yet implemented")
        override fun håndter(sammenligningsgrunnlagMessage: SammenligningsgrunnlagMessage) = TODO("Not yet implemented")
        override fun håndter(avviksvurderingerFraSpleisMessage: AvviksvurderingerFraSpleisMessage) {
            håndterteAvviksvurderinger++
        }
    }

    init {
        AvviksvurderingerFraSpleisRiver(testRapid, messageHandler)
    }

    @Test
    fun `kan lese avviksvurderinger`() {
        testRapid.sendTestMessage(avviksvurderingerMelding)
        assertEquals(1, messageHandler.håndterteAvviksvurderinger)
    }

    @Language("JSON")
    private val avviksvurderingerMelding = """
        {
          "@event_name": "avviksvurderinger",
          "fødselsnummer": "12345678910",
          "skjæringstidspunkter": [
            {
              "skjæringstidspunkt": "2018-01-01",
              "vurderingstidspunkt": "2018-01-01T00:00:00.000",
              "type": "SPLEIS",
              "omregnedeÅrsinntekter": [
                {
                  "orgnummer": "987654321",
                  "beløp": 30000.0
                }
              ],
              "sammenligningsgrunnlag": [
                {
                  "orgnummer": "987654321",
                  "skatteopplysninger": [
                    {
                      "beløp": 20000.0,
                      "måned": "2018-01",
                      "type": "LØNNSINNTEKT",
                      "fordel": "naturalytelse",
                      "beskrivelse": "skattepliktigDelForsikringer"
                    },
                    {
                      "beløp": 30000.0,
                      "måned": "2018-02",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                    }
                  ]
                }
              ]
            },
            {
              "skjæringstidspunkt": "2018-02-01",
              "vurderingstidspunkt": "1970-01-01T00:00:00",
              "type": "INFOTRYGD",
              "omregnedeÅrsinntekter": [],
              "sammenligningsgrunnlag": []
            },
            {
              "skjæringstidspunkt": "2018-03-01",
              "vurderingstidspunkt": "2018-03-01T00:00:00.000",
              "type": "SPLEIS",
              "omregnedeÅrsinntekter": [
                {
                  "orgnummer": "987654321",
                  "beløp": 30000.0
                }
              ],
              "sammenligningsgrunnlag": [
                {
                  "orgnummer": "987654321",
                  "skatteopplysninger": [
                    {
                      "beløp": 15000.0,
                      "måned": "2017-10",
                      "type": "LØNNSINNTEKT",
                      "fordel": "naturalytelse",
                      "beskrivelse": "skattepliktigDelForsikringer"
                    },
                    {
                      "beløp": 15000.0,
                      "måned": "2017-11",
                      "type": "LØNNSINNTEKT",
                      "fordel": "naturalytelse",
                      "beskrivelse": "skattepliktigDelForsikringer"
                    },
                    {
                      "beløp": 50000.0,
                      "måned": "2017-12",
                      "type": "LØNNSINNTEKT",
                      "fordel": "kontantytelse",
                      "beskrivelse": "fastloenn"
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()
}