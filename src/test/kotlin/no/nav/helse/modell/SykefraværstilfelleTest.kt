package no.nav.helse.modell

import io.mockk.spyk
import io.mockk.verify
import no.nav.helse.helpers.januar
import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SykefraværstilfelleTest {

    @Test
    fun `send ut behov dersom sykefraværstilfellet mangler sammenligningsgrunnlag`() {
        val sykefraværstilfelle = Sykefraværstilfelle.nyttTilfelle(1.januar)
        val beregningsgrunnlagSpy = spyk(omregnedeÅrsinntekter("a1" to 200000.0))
        sykefraværstilfelle.register(observer)
        sykefraværstilfelle.håndter(beregningsgrunnlagSpy)
        verify(exactly = 0) { beregningsgrunnlagSpy.beregnAvvik(any()) }
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