package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.Avviksvurderingsresultat.AvvikVurdert
import no.nav.helse.avviksvurdering.Avviksvurderingsresultat.TrengerIkkeNyVurdering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class Kilde {
    SPLEIS, SPINNVILL, INFOTRYGD
}

class Avviksvurderingsgrunnlag(
    val id: UUID,
    val fødselsnummer: Fødselsnummer,
    val skjæringstidspunkt: LocalDate,
    beregningsgrunnlag: IBeregningsgrunnlag,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val opprettet: LocalDateTime,
    val kilde: Kilde,
) {
    var beregningsgrunnlag: IBeregningsgrunnlag = beregningsgrunnlag
        private set
    private val MAKSIMALT_TILLATT_AVVIK = Avviksprosent(25.0)

    @Deprecated("Kun brukt i test, skal dø")
    internal fun avviksvurdering(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsresultat {
        if (kilde == Kilde.INFOTRYGD || this.beregningsgrunnlag.erLikt(beregningsgrunnlag))
            return TrengerIkkeNyVurdering(this)

        this.beregningsgrunnlag = beregningsgrunnlag
        val avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        return AvvikVurdert(
            vurdering = Avviksvurdering(
                id = this.id,
                harAkseptabeltAvvik = avviksprosent <= MAKSIMALT_TILLATT_AVVIK,
                avviksprosent = avviksprosent.avrundetTilFireDesimaler,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                maksimaltTillattAvvik = MAKSIMALT_TILLATT_AVVIK.avrundetTilFireDesimaler
            ),
        )
    }

    fun avviksvurdering(): Avviksvurdering {
        val beregningsgrunnlag = this.beregningsgrunnlag
        check(beregningsgrunnlag is Beregningsgrunnlag)
        val avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        return Avviksvurdering(
            id = this.id,
            harAkseptabeltAvvik = avviksprosent <= MAKSIMALT_TILLATT_AVVIK,
            avviksprosent = avviksprosent.avrundetTilFireDesimaler,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            maksimaltTillattAvvik = MAKSIMALT_TILLATT_AVVIK.avrundetTilFireDesimaler
        )
    }

    internal fun harLiktBeregningsgrunnlagSom(beregningsgrunnlag: Beregningsgrunnlag): Boolean {
        return this.beregningsgrunnlag.erLikt(beregningsgrunnlag)
    }

    internal fun gjenbrukSammenligningsgrunnlag(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsgrunnlag {
        return nyttGrunnlag(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag
        )
    }

    internal companion object {
        internal fun nyttGrunnlag(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            beregningsgrunnlag: IBeregningsgrunnlag = Ingen
        ) = Avviksvurderingsgrunnlag(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            opprettet = LocalDateTime.now(),
            kilde = Kilde.SPINNVILL
        )

        internal fun Collection<Avviksvurderingsgrunnlag>.siste() = maxByOrNull { it.opprettet }
    }
}
