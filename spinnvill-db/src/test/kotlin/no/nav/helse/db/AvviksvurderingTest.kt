package no.nav.helse.db

import no.nav.helse.TestDatabase
import no.nav.helse.dto.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AvviksvurderingTest {

    val database = TestDatabase.database()
    private val avviksvurdering = Avviksvurdering()

    @Test
    fun `Kan lage en avviksvurdering`() {
        val fødselsnummer = Fødselsnummer("12345678910")
        val skjæringstidspunkt = 1.januar
        val sammenligningsgrunnlag: Map<Organisasjonsnummer, Map<InntektPerMåned, Pair<Måned, År>>> = mapOf(
            Organisasjonsnummer("123456789") to mapOf(InntektPerMåned(20000.0) to Pair(Måned(1), År(2020)))
        )

        val avviksvurdering = avviksvurdering.upsert(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag, emptyMap())

        assertEquals(fødselsnummer, avviksvurdering.fødselsnummer)
        assertEquals(skjæringstidspunkt, avviksvurdering.skjæringstidspunkt)
        assertEquals(sammenligningsgrunnlag.entries.first().key, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().key)
        assertEquals(sammenligningsgrunnlag.entries.first().value, avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.entries.first().value)
        assertNull(avviksvurdering.beregningsgrunnlag)
    }
}