package no.nav.helse.modell

import no.nav.helse.modell.avviksvurdering.Avviksvurderinger
import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.modell.avviksvurdering.Sammenligningsgrunnlag
import java.time.LocalDate
import java.time.YearMonth

class Sykefraværstilfelle private constructor(
    private val skjæringstidspunkt: LocalDate,
    private var sammenligningsgrunnlag: Sammenligningsgrunnlag,
    private val avviksvurderinger: Avviksvurderinger
) {
    private val observers = mutableListOf<BehovObserver>()

    internal fun register(observer: BehovObserver) {
        observers.add(observer)
    }

    fun nyttUtkastTilVedtak(beregningsgrunnlag: Beregningsgrunnlag) {
        if (sammenligningsgrunnlag == Sammenligningsgrunnlag.IKKE_INNHENTET) trengerSammenligningsgrunnlag()
        avviksvurderinger.håndter(beregningsgrunnlag, sammenligningsgrunnlag)
    }

    private fun trengerSammenligningsgrunnlag() {
        observers.forEach {
            it.sammenligningsgrunnlag(nyttBehov(skjæringstidspunkt))
        }
    }

    private fun nyttBehov(skjæringstidspunkt: LocalDate): BehovForSammenligningsgrunnlag {
        val beregningsperiodeTom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        return BehovForSammenligningsgrunnlag(beregningsperiodeTom.minusMonths(11), beregningsperiodeTom)
    }

    internal companion object {
        internal fun nyttSykefraværstilfelle(skjæringstidspunkt: LocalDate) = Sykefraværstilfelle(
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = Sammenligningsgrunnlag.IKKE_INNHENTET,
            avviksvurderinger = Avviksvurderinger.ny()
        )
    }
}

