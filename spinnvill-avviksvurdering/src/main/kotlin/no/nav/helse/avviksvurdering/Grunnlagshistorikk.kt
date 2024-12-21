package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.Avviksvurderingsgrunnlag.Companion.siste
import java.time.LocalDate
import java.time.YearMonth

class Grunnlagshistorikk(
    private val fødselsnummer: Fødselsnummer,
    private val skjæringstidspunkt: LocalDate,
    grunnlagene: List<Avviksvurderingsgrunnlag>
) {
    private val grunnlagene = grunnlagene.toMutableList()
    private val siste get() = grunnlagene.siste()

    fun accept(visitor: Visitor) {
        grunnlagene.forEach { it.accept(visitor) }
    }

    fun håndterNytt(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsresultat {
        val sisteGrunnlag =
            siste
            ?: return Avviksvurderingsresultat.TrengerSammenligningsgrunnlag(behovForSammenligningsgrunnlag())
        val gjeldendeGrunnlag =
            if (sisteGrunnlag.trengerNyVurdering(beregningsgrunnlag)) sisteGrunnlag.lagNyttGrunnlag()
            else sisteGrunnlag

        if (sisteGrunnlag != gjeldendeGrunnlag) nyttSisteGrunnlag(gjeldendeGrunnlag)
        return gjeldendeGrunnlag.vurderAvvik(beregningsgrunnlag)
    }

    fun håndterNytt(sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        check(grunnlagene.isEmpty()) { "Forventer ikke å hente inn nytt sammenligningsgrunnlag hvis det tidligere er gjort en avviksvurdering" }
        val ny = Avviksvurderingsgrunnlag.nyttGrunnlag(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
        nyttSisteGrunnlag(ny)
    }

    private fun nyttSisteGrunnlag(avviksvurderingsgrunnlag: Avviksvurderingsgrunnlag) {
        grunnlagene.addLast(avviksvurderingsgrunnlag)
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
