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
    private var beregningsgrunnlag: IBeregningsgrunnlag,
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
        if (!this.beregningsgrunnlag.erForskjelligFra(beregningsgrunnlag)) return
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

    fun trengerNyVurdering(beregningsgrunnlag: Beregningsgrunnlag): Boolean {
        return when {
            this.kilde == Kilde.INFOTRYGD -> false
            this.beregningsgrunnlag is Ingen -> false
            this.beregningsgrunnlag.erForskjelligFra(beregningsgrunnlag) -> true
            else -> false
        }
    }

    fun nyAvviksvurdering(): Avviksvurdering {
        return nyAvviksvurdering(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
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
            beregningsgrunnlag = Ingen,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            opprettet = LocalDateTime.now(),
            kilde = Kilde.SPINNVILL
        )

        internal fun Collection<Avviksvurdering>.siste() = maxByOrNull { it.opprettet }
    }
}