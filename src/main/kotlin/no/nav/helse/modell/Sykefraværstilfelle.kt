package no.nav.helse.modell

import no.nav.helse.modell.avviksvurdering.Sammenligningsgrunnlag
import java.time.LocalDate
import java.time.YearMonth

class Sykefraværstilfelle(
    val skjæringstidspunkt: LocalDate,
    val omregnetÅrsinntekt: Double
) {

    fun nyttUtkastTilVedtak(sammenligningsgrunnlag: Sammenligningsgrunnlag?): BehovForSammenligningsgrunnlag? {
        val beregningsperiodeTom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        if (sammenligningsgrunnlag == null) {
            return BehovForSammenligningsgrunnlag(beregningsperiodeTom.minusMonths(11), beregningsperiodeTom)
        }
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