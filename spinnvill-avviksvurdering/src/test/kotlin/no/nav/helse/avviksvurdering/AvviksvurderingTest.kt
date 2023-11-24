package no.nav.helse.avviksvurdering

import no.nav.helse.KriterieObserver
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.Organisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvviksvurderingTest {

    @Test
    fun `har gjort avviksvurdering før`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering(sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(beregningsgrunnlag("a1" to 600000.0))
        observer.clear()
        avviksvurdering.håndter(beregningsgrunnlag("a1" to 600000.0))
        assertEquals(0, observer.avviksvurderinger.size)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik innenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering(sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(beregningsgrunnlag("a1" to 600000.0))
        assertEquals(1, observer.avviksvurderinger.size)
        val (harAkseptabeltAvvik, avviksprosent) = observer.avviksvurderinger.single()
        assertEquals(true, harAkseptabeltAvvik)
        assertEquals(0.0, avviksprosent)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik utenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering(sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(beregningsgrunnlag("a1" to 360000.0))
        assertEquals(1, observer.avviksvurderinger.size)
        val (harAkseptabeltAvvik, avviksprosent) = observer.avviksvurderinger.single()
        assertEquals(false, harAkseptabeltAvvik)
        assertEquals(40.0, avviksprosent)
    }


    private fun sammenligningsgrunnlag(inntekt: Double) = Sammenligningsgrunnlag(
        listOf(
            ArbeidsgiverInntekt(Organisasjonsnummer("a1"), List(12) { inntekt })
        )
    )

    private fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) =
        Beregningsgrunnlag.opprett(arbeidsgivere.toMap().entries.associate { Organisasjonsnummer(it.key) to OmregnetÅrsinntekt(it.value) })

    private val observer = object : KriterieObserver {

        val avviksvurderinger = mutableListOf<Pair<Boolean, Double>>()

        fun clear() {
            avviksvurderinger.clear()
        }

        override fun avvikVurdert(harAkseptabeltAvvik: Boolean, avviksprosent: Double) {
            avviksvurderinger.add(harAkseptabeltAvvik to avviksprosent)
        }
    }
}

