package no.nav.helse.modell

import no.nav.helse.helpers.januar
import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SykefraværstilfelleTest {

    @Test
    fun `send ut behov dersom sykefraværstilfellet mangler sammenligningsgrunnlag`() {
        val sykefraværstilfelle = Sykefraværstilfelle.nyttSykefraværstilfelle(1.januar)
        sykefraværstilfelle.register(observer)
        sykefraværstilfelle.nyttUtkastTilVedtak(omregnedeÅrsinntekter("a1" to 200000.0))
        assertEquals(1, observer.behov.size)
        val behov = observer.behov.single()
        assertEquals(YearMonth.of(2017, 1), behov.beregningsperiodeFom)
        assertEquals(YearMonth.of(2017, 12), behov.beregningsperiodeTom)
    }

    private val observer = object : BehovObserver {
        val behov = mutableListOf<BehovForSammenligningsgrunnlag>()

        override fun sammenligningsgrunnlag(behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) {
            behov.add(behovForSammenligningsgrunnlag)
        }
    }

    private fun omregnedeÅrsinntekter(vararg arbeidsgivere: Pair<String, Double>) =
        Beregningsgrunnlag(arbeidsgivere.toMap())
}