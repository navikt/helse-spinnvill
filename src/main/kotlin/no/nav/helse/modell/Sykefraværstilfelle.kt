package no.nav.helse.modell

import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import java.time.LocalDate

class Sykefraværstilfelle(
    private val skjæringstidspunkt: LocalDate,
    private var avviksvurdering: Avviksvurdering
) {

    fun nyttUtkastTilVedtak(beregningsgrunnlag: Beregningsgrunnlag) {
        avviksvurdering.håndter(skjæringstidspunkt, beregningsgrunnlag)
    }
}