package no.nav.helse.modell.avviksvurdering

import no.nav.helse.helpers.januar
import no.nav.helse.modell.BehovForSammenligningsgrunnlag
import no.nav.helse.modell.BehovObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AvviksvurderingTest{

    @Test
    fun`har ikke gjort avviksvurdering og mangler sammenligningsgrunnlag` () {
        val avviksvurdering = Avviksvurdering
            .nyAvviksvurdering(omregnedeÅrsinntekter("a1" to 600000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(1.januar, omregnedeÅrsinntekter("a1" to 600000.0))
        assertEquals(1, observer.behov.size)
    }
    @Test
    fun`har ikke gjort avviksvurdering men har sammenligningsgrunnlag` () {
        val avviksvurdering = Avviksvurdering
            .nyAvviksvurdering(omregnedeÅrsinntekter("a1" to 600000.0))
            .nyttSammenligningsgrunnlag(sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(1.januar, omregnedeÅrsinntekter("a1" to 600000.0))
        assertEquals(0, observer.behov.size)
    }


    private fun sammenligningsgrunnlag(inntekt: Double) = Sammenligningsgrunnlag(List(12) { inntekt })

    private fun omregnedeÅrsinntekter(vararg arbeidsgivere: Pair<String, Double>) = Beregningsgrunnlag(arbeidsgivere.toMap())

    private val observer = object: BehovObserver {

        val behov = mutableListOf<BehovForSammenligningsgrunnlag>()

        override fun sammenligningsgrunnlag(behovForSammenligningsgrunnlag: BehovForSammenligningsgrunnlag) {
            behov.add(behovForSammenligningsgrunnlag)
        }
    }
}

