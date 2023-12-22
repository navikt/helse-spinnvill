package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class Kilde {
    SPLEIS, SPINNVILL, INFOTRYGD
}

class Avviksvurdering(
    private val id: UUID,
    private val fødselsnummer: Fødselsnummer,
    private val skjæringstidspunkt: LocalDate,
    private var beregningsgrunnlag: Beregningsgrunnlag,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    private val opprettet: LocalDateTime,
    private val kilde: Kilde
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN

    private val observers = mutableListOf<KriterieObserver>()

    fun register(vararg observers: KriterieObserver) {
        this.observers.addAll(observers)
    }

    fun håndter(beregningsgrunnlag: Beregningsgrunnlag) {
        if (kilde == Kilde.INFOTRYGD) return
        if (beregningsgrunnlag == this.beregningsgrunnlag) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        observers.forEach {
            it.avvikVurdert(
                id = id,
                harAkseptabeltAvvik = avviksprosent.harAkseptabeltAvvik(),
                avviksprosent = avviksprosent.avrundetTilToDesimaler(),
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                maksimaltTillattAvvik = Avviksprosent.MAKSIMALT_TILLATT_AVVIK.avrundetTilToDesimaler()
            )
        }
    }

    fun accept(visitor: Visitor) {
        visitor.visitAvviksvurdering(id, fødselsnummer, skjæringstidspunkt, kilde, opprettet)
        beregningsgrunnlag.accept(visitor)
        sammenligningsgrunnlag.accept(visitor)
    }

    fun vurderBehovForNyVurdering(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurdering {
        if (this.kilde == Kilde.INFOTRYGD) return this
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
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            opprettet = LocalDateTime.now(),
            kilde = Kilde.SPINNVILL
        )

        internal fun Collection<Avviksvurdering>.siste() = maxByOrNull { it.opprettet }
    }
}