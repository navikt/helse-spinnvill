package no.nav.helse.avviksvurdering

import no.nav.helse.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.test.assertEquals

class AvviksvurderingerTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val arbeidsgiver = "987654321"
    private val skjæringstidspunkt = 1.januar

    @Test
    fun `be om sammenligningsgrunnlag hvis det ikke er gjort noen avviksvurderinger enda`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        val behov = behovObserver.behov.firstOrNull()
        assertEquals(0, kriterieObserver.avvikVurdert.size)
        assertEquals(1, behovObserver.behov.size)
        assertEquals(1.januar, behov?.skjæringstidspunkt)
        assertEquals(YearMonth.of(2017, 1), behov?.beregningsperiodeFom)
        assertEquals(YearMonth.of(2017, 12), behov?.beregningsperiodeTom)
    }

    @Test
    fun `gjør avviksvurdering når vi kun har sammenligningsgrunnlag`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertEquals(1, kriterieObserver.avvikVurdert.size)
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag er likt`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        kriterieObserver.reset()
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertEquals(0, kriterieObserver.avvikVurdert.size)
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag er forskjellig`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        kriterieObserver.reset()
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 500000.0))
        assertEquals(1, kriterieObserver.avvikVurdert.size)
        assertEquals(0, behovObserver.behov.size)
    }

    private val behovObserver = object : BehovObserver {
        val behov = mutableListOf<BehovForSammenligningsgrunnlag>()
        override fun sammenligningsgrunnlag(behov: BehovForSammenligningsgrunnlag) {
            this.behov.add(behov)
        }
    }

    private val kriterieObserver = object : KriterieObserver {
        fun reset() = avvikVurdert.clear()
        val avvikVurdert = mutableListOf<UUID>()
        override fun avvikVurdert(
            id: UUID,
            harAkseptabeltAvvik: Boolean,
            avviksprosent: Double,
            beregningsgrunnlag: Beregningsgrunnlag,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            maksimaltTillattAvvik: Double
        ) {
            avvikVurdert.add(id)
        }
    }

    private fun avviksvurderinger() = Avviksvurderinger(
        fødselsnummer,
        skjæringstidspunkt,
        emptyList()
    ).also {
        it.registrer(behovObserver)
        it.registrer(kriterieObserver)
    }

    private fun beregningsgrunnlag(vararg arbeidsgivere: Pair<String, Double>) =
        Beregningsgrunnlag.opprett(arbeidsgivere.toMap().entries.associate { Arbeidsgiverreferanse(it.key) to OmregnetÅrsinntekt(it.value) })

    private fun sammenligningsgrunnlag(
        skjæringstidspunkt: LocalDate,
        vararg arbeidsgivere: Pair<String, Double>
    ) =
        Sammenligningsgrunnlag(
            arbeidsgivere.map {(arbeidsgiver, inntektPerÅr) ->
                ArbeidsgiverInntekt(
                    arbeidsgiverreferanse = arbeidsgiver.somArbeidsgiverref(),
                    inntekter = List(12) {
                        ArbeidsgiverInntekt.MånedligInntekt(
                            InntektPerMåned(value = inntektPerÅr/12),
                            måned = YearMonth.from(skjæringstidspunkt.minusMonths(it.toLong())),
                            null,
                            null,
                            ArbeidsgiverInntekt.Inntektstype.LØNNSINNTEKT
                        )
                    }
                )
            }
        )
}