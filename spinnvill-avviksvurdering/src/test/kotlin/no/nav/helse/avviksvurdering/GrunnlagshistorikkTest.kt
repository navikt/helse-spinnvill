package no.nav.helse.avviksvurdering

import no.nav.helse.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GrunnlagshistorikkTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val arbeidsgiver = "987654321"
    private val skjæringstidspunkt = 1.januar

    @Test
    fun `be om sammenligningsgrunnlag hvis det ikke er gjort noen avviksvurderinger enda`() {
        val avviksvurderinger = avviksvurderinger()
        val resultat = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertIs<Avviksvurderingsresultat.TrengerSammenligningsgrunnlag>(resultat)
        assertEquals(1.januar, resultat.behov.skjæringstidspunkt)
        assertEquals(YearMonth.of(2017, 1), resultat.behov.beregningsperiodeFom)
        assertEquals(YearMonth.of(2017, 12), resultat.behov.beregningsperiodeTom)
    }

    @Test
    fun `gjør avviksvurdering når vi kun har sammenligningsgrunnlag`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        val resultat = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag er likt`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        val resultat = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat)
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og mottar beregningsgrunnlag med beløp 0 kr`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        val resultat = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 0.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag er forskjellig`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))
        val resultat = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 500000.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag bare er litt forskjellig`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.håndterNytt(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0))
        avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.0))

        val resultat1 = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 600000.1))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat1)

        val resultat2 = avviksvurderinger.håndterNytt(beregningsgrunnlag(arbeidsgiver to 599999.999999994))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat2)
    }

    private fun avviksvurderinger() = Grunnlagshistorikk(
        fødselsnummer,
        skjæringstidspunkt,
        emptyList()
    )

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