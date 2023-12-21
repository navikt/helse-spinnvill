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

    internal fun registrer(observer: BehovObserver) {
        behovObservers.add(observer)
    }

    internal fun registrer(observer: KriterieObserver) {
        kriterieObservers.add(observer)
    }

    internal fun håndterNytt(beregningsgrunnlag: Beregningsgrunnlag) {
        val siste = siste ?: return behovForSammenligningsgrunnlag()
        val gjeldende = siste.vurderBehovForNyVurdering(beregningsgrunnlag)
        if (siste != gjeldende) nyAvviksvurdering(gjeldende)
        gjeldende.håndter(beregningsgrunnlag)
    }

    internal fun håndterNytt(sammenligningsgrunnlag: Sammenligningsgrunnlag) {
        check(avviksvurderinger.isEmpty()) { "Forventer ikke å hente inn nytt sammenligningsgrunnlag hvis det tidligere er gjort en avviksvurdering" }
        val ny = Avviksvurdering.nyAvviksvurdering(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
        nyAvviksvurdering(ny)
    }

    private fun nyAvviksvurdering(avviksvurdering: Avviksvurdering) {
        kriterieObservers.forEach { observer ->
            avviksvurdering.register(observer)
        }
        avviksvurderinger.add(avviksvurdering)
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