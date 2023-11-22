package no.nav.helse.db

import no.nav.helse.TestDatabase
import no.nav.helse.helpers.januar
import org.jetbrains.exposed.sql.transactions.transaction
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

        val enAvviksvurdering = avviksvurdering.upsert(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag, emptyMap())

        transaction {
            assertEquals(fødselsnummer, Fødselsnummer(enAvviksvurdering.fødselsnummer))
            assertEquals(skjæringstidspunkt, enAvviksvurdering.skjæringstidspunkt)
            assertEquals(sammenligningsgrunnlag.entries.first().key, Organisasjonsnummer(enAvviksvurdering.sammenligningsgrunnlag.first().organisasjonsnummer))
            assertEquals(sammenligningsgrunnlag.entries.first().value, enAvviksvurdering.sammenligningsgrunnlag.first().inntekter.associate { InntektPerMåned(it.inntekt) to Pair(Måned(it.måned), År(it.år)) })
            assertNull(enAvviksvurdering.beregningsgrunnlag)
        }
    }
}