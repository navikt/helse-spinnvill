package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Avviksvurderingsgrunnlag(
    val id: UUID,
    val fødselsnummer: Fødselsnummer,
    val skjæringstidspunkt: LocalDate,
    val beregningsgrunnlag: Beregningsgrunnlag,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val opprettet: LocalDateTime,
) {
    private val MAKSIMALT_TILLATT_AVVIK = Avviksprosent(25.0)

    fun avviksvurdering(): Avviksvurdering {
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

    fun beregningsgrunnlagLiktSom(beregningsgrunnlag: Beregningsgrunnlag): Boolean {
        return this.beregningsgrunnlag.erLikt(beregningsgrunnlag)
    }

    fun kopierMedNyttBeregningsgrunnlag(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsgrunnlag {
        return nyttGrunnlag(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag
        )
    }

    companion object {
        fun nyttGrunnlag(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            beregningsgrunnlag: Beregningsgrunnlag
        ) = Avviksvurderingsgrunnlag(
            id = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            opprettet = LocalDateTime.now()
        )
    }
}
