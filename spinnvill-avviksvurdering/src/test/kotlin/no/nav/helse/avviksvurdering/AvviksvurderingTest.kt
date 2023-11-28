package no.nav.helse.avviksvurdering

import no.nav.helse.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class AvviksvurderingTest {

    @Test
    fun `har gjort avviksvurdering før`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(beregningsgrunnlag("a1" to 600000.0))
        observer.clear()
        avviksvurdering.håndter(beregningsgrunnlag("a1" to 600000.0))
        assertEquals(0, observer.avviksvurderinger.size)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik innenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(beregningsgrunnlag("a1" to 600000.0))
        assertEquals(1, observer.avviksvurderinger.size)
        val (harAkseptabeltAvvik, avviksprosent) = observer.avviksvurderinger.single()
        assertEquals(true, harAkseptabeltAvvik)
        assertEquals(0.0, avviksprosent)
    }

    @Test
    fun `har ikke gjort avviksvurdering før og avvik utenfor akseptabelt avvik`() {
        val avviksvurdering = Avviksvurdering.nyAvviksvurdering("12345678910".somFnr(), 1.januar, sammenligningsgrunnlag(50000.0))
        avviksvurdering.register(observer)

        avviksvurdering.håndter(beregningsgrunnlag("a1" to 360000.0))
        assertEquals(1, observer.avviksvurderinger.size)
        val (harAkseptabeltAvvik, avviksprosent) = observer.avviksvurderinger.single()
        assertEquals(false, harAkseptabeltAvvik)
        assertEquals(40.0, avviksprosent)
    }

    @Disabled
    @Test
    fun `sender ut varsel ved mer enn 25pp avvik`() {

    }


    private fun sammenligningsgrunnlag(inntekt: Double) = Sammenligningsgrunnlag(
        listOf(
            ArbeidsgiverInntekt(Arbeidsgiverreferanse("a1"), List(12) {
                ArbeidsgiverInntekt.MånedligInntekt(
                    inntekt = InntektPerMåned(inntekt),
                    måned = YearMonth.of(2018, it + 1),
                    fordel = Fordel("En fordel"),
                    beskrivelse = Beskrivelse("En beskrivelse"),
                    inntektstype = ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                )
            })
        )
    )

    private fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) =
        Beregningsgrunnlag.opprett(arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) })

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

