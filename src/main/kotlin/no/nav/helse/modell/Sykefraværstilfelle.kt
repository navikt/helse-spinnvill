package no.nav.helse.modell

class Sykefraværstilfelle {

    fun nyttUtkastTilVedtak(): String? {
        return null
    }

    fun løsningPåBehov(løsning: String): Utfall {
        //vurder avvik
        //lag varsel ved avvik
        //subsumsjonsmelding
        val subsumsjonsmelding = "sumsums"
        val varsel = null
        return Utfall(subsumsjonsmelding, varsel)
    }
}