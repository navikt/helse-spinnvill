package no.nav.helse.modell

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ModellTest {

    @Test
    fun `behandler utkast_til_vedtak og videresender til Spesialist`() {
        val sykefraværstilfelle = Sykefraværstilfelle()
        val behov = sykefraværstilfelle.nyttUtkastTilVedtak()
        val løsning: String = behov.gjørOmTilSvar()
        val utfall = sykefraværstilfelle.løsningPåBehov(løsning)
        assertEquals("sumsums", utfall.subsumsjonsmelding)
    }
}

private fun String?.gjørOmTilSvar(): String {
    return ""
}
