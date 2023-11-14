package no.nav.helse.modell.avviksvurdering

import no.nav.helse.modell.BehovForSammenligningsgrunnlag
import no.nav.helse.modell.BehovObserver
import java.time.LocalDate
import java.time.YearMonth

class Avviksvurdering private constructor(
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag
) {

    private val observers = mutableListOf<BehovObserver>()

    internal fun register(observer: BehovObserver) {
        observers.add(observer)
    }

    internal fun nyttSammenligningsgrunnlag(sammenligningsgrunnlag: Sammenligningsgrunnlag) =
        Avviksvurdering(this.beregningsgrunnlag, sammenligningsgrunnlag)

    internal fun håndter(skjæringstidspunkt: LocalDate, beregningsgrunnlag: Beregningsgrunnlag) {
        if (sammenligningsgrunnlag == Sammenligningsgrunnlag.IKKE_INNHENTET)
            return trengerSammenligningsgrunnlag(skjæringstidspunkt)
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        TODO("gjør avviksvurdering")
    }

    private fun nyttBehov(skjæringstidspunkt: LocalDate): BehovForSammenligningsgrunnlag {
        val beregningsperiodeTom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        return BehovForSammenligningsgrunnlag(beregningsperiodeTom.minusMonths(11), beregningsperiodeTom)
    }

    private fun trengerSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) {
        observers.forEach {
            it.sammenligningsgrunnlag(nyttBehov(skjæringstidspunkt))
        }
    }

    internal companion object {
        internal fun nyAvviksvurdering(beregningsgrunnlag: Beregningsgrunnlag) = Avviksvurdering(beregningsgrunnlag, Sammenligningsgrunnlag.IKKE_INNHENTET)
    }
}