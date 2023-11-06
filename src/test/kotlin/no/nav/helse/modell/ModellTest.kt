package no.nav.helse.modell

import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.test.assertEquals

class ModellTest {

    @Test
    fun `ber om opplysninger ved manglende sammenligningsgrunnlag`() {
        val sykefraværstilfelle = Sykefraværstilfelle(1.januar, 200000.0)
        val behov = sykefraværstilfelle.nyttUtkastTilVedtak(null)!!
        val forventetBehov = BehovForSammenligningsgrunnlag(YearMonth.of(2017, 1), YearMonth.of(2017, 12))
        assertEquals(forventetBehov, behov)
    }

    @Test
    fun `ber om opplysninger ved manglende sammenligningsgrunnlag `() {
        val sykefraværstilfelle = Sykefraværstilfelle(1.januar, 200000.0)
        val behov = sykefraværstilfelle.nyttUtkastTilVedtak(null)!!
        val løsning: String = behov.gjørOmTilSvar()
        val utfall = sykefraværstilfelle.løsningPåBehov(løsning)
        assertEquals("sumsums", utfall.subsumsjonsmelding)
    }
}

private fun BehovForSammenligningsgrunnlag.gjørOmTilSvar(): String {
    return ""
}
