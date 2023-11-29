package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
import java.time.LocalDate
import java.util.*

class Avviksvurdering(
    private val id: UUID,
    private val fødselsnummer: Fødselsnummer,
    private val skjæringstidspunkt: LocalDate,
    private var beregningsgrunnlag: Beregningsgrunnlag,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN

    private val observers = mutableListOf<KriterieObserver>()

    fun register(vararg observers: KriterieObserver) {
        this.observers.addAll(observers)
    }

    fun håndter(beregningsgrunnlag: Beregningsgrunnlag) {
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        observers.forEach {
            it.avvikVurdert(avviksprosent.harAkseptabeltAvvik(), avviksprosent.avrundetTilToDesimaler())
        }
    }

    fun accept(visitor: Visitor) {
        visitor.visitAvviksvurdering(id, fødselsnummer, skjæringstidspunkt)
        beregningsgrunnlag.accept(visitor)
        sammenligningsgrunnlag.accept(visitor)
    }

    fun vurderBehovForNyVurdering(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurdering {
        if (this.beregningsgrunnlag == Beregningsgrunnlag.INGEN) return this
        if (this.beregningsgrunnlag != beregningsgrunnlag) return nyAvviksvurdering(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
        return this
    }

    internal companion object {
        internal fun nyAvviksvurdering(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
            sammenligningsgrunnlag: Sammenligningsgrunnlag
        ) = Avviksvurdering(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = Beregningsgrunnlag.INGEN,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )
    }
}