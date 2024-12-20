package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
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

    private val behovObservers = mutableListOf<BehovObserver>()
    private val kriterieObservers = mutableListOf<KriterieObserver>()

    fun registrer(observer: BehovObserver) {
        behovObservers.add(observer)
    }

    fun registrer(vararg observers: KriterieObserver) {
        kriterieObservers.addAll(observers)
        avviksvurderinger.forEach { it.register(*observers) }
    }

    fun accept(visitor: Visitor) {
        avviksvurderinger.forEach { it.accept(visitor) }
    }

    fun håndterNytt(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurdering? {
        val siste = siste ?: run {
            behovForSammenligningsgrunnlag()
            return null
        }
        val gjeldende = if (siste.trengerNyVurdering(beregningsgrunnlag)) siste.nyAvviksvurdering() else siste

        if (siste != gjeldende) nyAvviksvurdering(gjeldende)
        gjeldende.håndter(beregningsgrunnlag)
        return gjeldende
    }

    fun håndterNytt(sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        check(avviksvurderinger.isEmpty()) { "Forventer ikke å hente inn nytt sammenligningsgrunnlag hvis det tidligere er gjort en avviksvurdering" }
        val ny = Avviksvurdering.nyAvviksvurdering(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
        nyAvviksvurdering(ny)
    }

    private fun nyAvviksvurdering(avviksvurdering: Avviksvurdering) {
        kriterieObservers.forEach { observer ->
            avviksvurdering.register(observer)
        }
        avviksvurderinger.addLast(avviksvurdering)
    }

    private fun behovForSammenligningsgrunnlag() {
        behovObservers.forEach {
            val tom = YearMonth.from(skjæringstidspunkt).minusMonths(1)
            val fom = tom.minusMonths(11)
            it.sammenligningsgrunnlag(
                BehovForSammenligningsgrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsperiodeFom = fom,
                    beregningsperiodeTom = tom
                )
            )
        }
    }
}