package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.Avviksvurdering.Companion.siste
import java.time.LocalDate
import java.time.YearMonth

class Avviksvurderinger(
    private val fødselsnummer: Fødselsnummer,
    private val skjæringstidspunkt: LocalDate,
    avviksvurderinger: List<Avviksvurdering>
) {
    private val avviksvurderinger = avviksvurderinger.toMutableList()
    private val siste get() = avviksvurderinger.siste()

    fun accept(visitor: Visitor) {
        avviksvurderinger.forEach { it.accept(visitor) }
    }

    fun håndterNytt(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsresultat {
        val sisteAvviksvurdering =
            siste
            ?: return Avviksvurderingsresultat.TrengerSammenligningsgrunnlag(behovForSammenligningsgrunnlag())
        val gjeldendeAvviksvurdering =
            if (sisteAvviksvurdering.trengerNyVurdering(beregningsgrunnlag)) sisteAvviksvurdering.lagNyAvviksvurdering()
            else sisteAvviksvurdering

        if (sisteAvviksvurdering != gjeldendeAvviksvurdering) nySisteAvviksvurdering(gjeldendeAvviksvurdering)
        return gjeldendeAvviksvurdering.vurderAvvik(beregningsgrunnlag)
    }

    fun håndterNytt(sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        check(avviksvurderinger.isEmpty()) { "Forventer ikke å hente inn nytt sammenligningsgrunnlag hvis det tidligere er gjort en avviksvurdering" }
        val ny = Avviksvurdering.nyAvviksvurdering(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
        nySisteAvviksvurdering(ny)
    }

    private fun nySisteAvviksvurdering(avviksvurdering: Avviksvurdering) {
        avviksvurderinger.addLast(avviksvurdering)
    }

    private fun behovForSammenligningsgrunnlag(): BehovForSammenligningsgrunnlag {
        val tom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        val fom = tom.minusMonths(11)
        val behov = BehovForSammenligningsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsperiodeFom = fom,
            beregningsperiodeTom = tom
        )
        return behov
    }
}
