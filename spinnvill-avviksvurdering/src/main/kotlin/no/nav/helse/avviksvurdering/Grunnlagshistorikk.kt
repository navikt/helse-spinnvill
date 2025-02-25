package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.Avviksvurderingsgrunnlag.Companion.siste
import no.nav.helse.avviksvurdering.Avviksvurderingsresultat.*
import java.time.LocalDate
import java.time.YearMonth

class Grunnlagshistorikk(
    private val fødselsnummer: Fødselsnummer,
    private val skjæringstidspunkt: LocalDate,
    grunnlagene: List<Avviksvurderingsgrunnlag>
) {
    private val grunnlagene = grunnlagene.toMutableList()
    private val sisteGrunnlag get() = grunnlagene.siste()

    fun grunnlagene(): List<Avviksvurderingsgrunnlag> = grunnlagene.toList()

    fun nyttBeregningsgrunnlag(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsresultat {
        val sisteAvviksvurderingsgrunnlag = sisteGrunnlag ?: return TrengerSammenligningsgrunnlag(behovForSammenligningsgrunnlag())
        if (sisteAvviksvurderingsgrunnlag.harLiktBeregningsgrunnlagSom(beregningsgrunnlag)) return TrengerIkkeNyVurdering(sisteAvviksvurderingsgrunnlag)

        val nyttGrunnlag = sisteAvviksvurderingsgrunnlag.gjenbrukSammenligningsgrunnlag(beregningsgrunnlag)
        return AvvikVurdert(nyttGrunnlag.utførNyAvviksvurdering())
    }


    fun nyttSammenligningsgrunnlag(sammenligningsgrunnlag: Sammenligningsgrunnlag, beregningsgrunnlag: Beregningsgrunnlag): Avviksvurdering {
        check(grunnlagene.isEmpty()) { "Forventer ikke å hente inn nytt sammenligningsgrunnlag hvis det tidligere er gjort en avviksvurdering" }
        val nyttGrunnlag = Avviksvurderingsgrunnlag.nyttGrunnlag(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag
        )
        return nyttGrunnlag.utførNyAvviksvurdering()
    }

    private fun Avviksvurderingsgrunnlag.utførNyAvviksvurdering(): Avviksvurdering {
        nyttSisteGrunnlag(this)
        return this.avviksvurdering()
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
