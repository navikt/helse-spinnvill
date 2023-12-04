package no.nav.helse.mediator.producer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.*
import no.nav.helse.avviksvurdering.ArbeidsgiverInntekt
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.helpers.januar
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class AvviksvurderingProducerTest {
    private val testRapid = TestRapid()
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val aktørId = AktørId("1234567891011")
    private val skjæringstidspunkt = 1.januar
    private val avviksvurderingProducer =
        AvviksvurderingProducer(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            rapidsConnection = testRapid
        )

    @Test
    fun `produser avviksvurdering for akseptebelt avvik`() {
        avviksvurderingProducer.avvikVurdert(
            avviksprosent = 24.9,
            harAkseptabeltAvvik = true,
            beregningsgrunnlag = Beregningsgrunnlag.INGEN,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            maksimaltTillattAvvik = 25.0
        )
        avviksvurderingProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }
    @Test
    fun `produser avviksvurdering for uakseptebelt avvik`() {
        avviksvurderingProducer.avvikVurdert(
            avviksprosent = 25.0,
            harAkseptabeltAvvik = false,
            beregningsgrunnlag = Beregningsgrunnlag.INGEN,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            maksimaltTillattAvvik = 25.0
        )
        avviksvurderingProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `avviksvurdering kø tømmes etter hver finalize`() {
        avviksvurderingProducer.avvikVurdert(
            avviksprosent = 24.9,
            harAkseptabeltAvvik = true,
            beregningsgrunnlag = Beregningsgrunnlag.INGEN,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            maksimaltTillattAvvik = 25.0
        )
        avviksvurderingProducer.finalize()
        avviksvurderingProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `ikke send ut avviksvurderingsmelding før finalize blir kalt`() {
        avviksvurderingProducer.avvikVurdert(
            avviksprosent = 24.9,
            harAkseptabeltAvvik = true,
            beregningsgrunnlag = Beregningsgrunnlag.INGEN,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            maksimaltTillattAvvik = 25.0
        )
        assertEquals(0, testRapid.inspektør.size)
        avviksvurderingProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
    }

    @Test
    fun `produserer riktig format på avviksvurderingmelding`() {
        avviksvurderingProducer.avvikVurdert(
            avviksprosent = 24.9,
            harAkseptabeltAvvik = true,
            beregningsgrunnlag = Beregningsgrunnlag.opprett(
                mapOf(
                    "987654321".somArbeidsgiverref() to OmregnetÅrsinntekt(
                        400000.0
                    )
                )),
            sammenligningsgrunnlag = Sammenligningsgrunnlag(listOf(ArbeidsgiverInntekt(
                arbeidsgiverreferanse = "987654321".somArbeidsgiverref(),
                inntekter = listOf(ArbeidsgiverInntekt.MånedligInntekt(
                    inntekt = InntektPerMåned(30000.0),
                    måned = YearMonth.from(1.januar),
                    fordel = null,
                    beskrivelse = null,
                    inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                ))
            ))),
            maksimaltTillattAvvik = 25.0
        )
        avviksvurderingProducer.finalize()
        assertEquals(1, testRapid.inspektør.size)
        val message = testRapid.inspektør.message(0)
        assertEquals("avviksvurdering", message["@event_name"].asText())
        assertEquals(fødselsnummer.value, message["fødselsnummer"].asText())
        assertEquals(aktørId.value, message["aktørId"].asText())
        assertEquals(skjæringstidspunkt, message["skjæringstidspunkt"].asLocalDate())
        assertPresent(message["avviksvurdering"])
        val avviksvurdering = message["avviksvurdering"]
        assertPresent(avviksvurdering["opprettet"])
        assertPresent(avviksvurdering["avviksprosent"])
        assertPresent(avviksvurdering["beregningsgrunnlag"])
        val beregningsgrunnlag = avviksvurdering["beregningsgrunnlag"]
        assertPresent(beregningsgrunnlag["totalbeløp"])
        assertPresent(beregningsgrunnlag["omregnedeÅrsinntekter"])
        val omregnedeÅrsinntekter = beregningsgrunnlag["omregnedeÅrsinntekter"]
        assertEquals(1, omregnedeÅrsinntekter.size())
        val enOmregnetÅrsinntekt = omregnedeÅrsinntekter.first()
        assertPresent(enOmregnetÅrsinntekt["arbeidsgiverreferanse"])
        assertPresent(enOmregnetÅrsinntekt["beløp"])
        assertPresent(avviksvurdering["sammenligningsgrunnlag"])
        val sammenligningsgrunnlag = avviksvurdering["sammenligningsgrunnlag"]
        assertPresent(sammenligningsgrunnlag["totalbeløp"])
        assertPresent(sammenligningsgrunnlag["innrapporterteInntekter"])
        val innrapporterteInntekter = sammenligningsgrunnlag["innrapporterteInntekter"]
        assertEquals(1, innrapporterteInntekter.size())
        val enInnrapportertInntekt = innrapporterteInntekter.first()
        assertPresent(enInnrapportertInntekt["arbeidsgiverreferanse"])
        assertPresent(enInnrapportertInntekt["inntekter"])
        val inntekter = enInnrapportertInntekt["inntekter"]
        assertEquals(1, inntekter.size())
        val enInntekt = inntekter.first()
        assertPresent(enInntekt["årMåned"])
        assertPresent(enInntekt["beløp"])
    }

    private fun assertPresent(jsonNode: JsonNode?) {
        Assertions.assertNotNull(jsonNode) { "Forventer at noden ikke er null" }
        jsonNode?.isMissingOrNull()?.let { Assertions.assertFalse(it) { "Forventer at noden ikke mangler" } }
    }
}