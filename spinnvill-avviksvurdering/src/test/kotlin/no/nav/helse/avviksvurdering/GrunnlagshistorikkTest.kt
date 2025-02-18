package no.nav.helse.avviksvurdering

import no.nav.helse.*
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GrunnlagshistorikkTest {
    private val fødselsnummer = Fødselsnummer("12345678910")
    private val arbeidsgiver = "987654321"
    private val skjæringstidspunkt = 1.januar

    @Test
    fun `be om sammenligningsgrunnlag hvis det ikke er gjort noen avviksvurderinger enda`() {
        val avviksvurderinger = avviksvurderinger()
        val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertIs<Avviksvurderingsresultat.TrengerSammenligningsgrunnlag>(resultat)
        assertEquals(1.januar, resultat.behov.skjæringstidspunkt)
        assertEquals(YearMonth.of(2017, 1), resultat.behov.beregningsperiodeFom)
        assertEquals(YearMonth.of(2017, 12), resultat.behov.beregningsperiodeTom)
    }

    @Test
    fun `gjør avviksvurdering når vi mottar sammenligningsgrunnlag`() {
        val avviksvurderinger = avviksvurderinger()
        val resultat = avviksvurderinger.nyttSammenligningsgrunnlag(
            sammenligningsgrunnlag = sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0),
            beregningsgrunnlag = beregningsgrunnlag(arbeidsgiver to 600000.0)
        )
        assertTrue(resultat.vurdering.harAkseptabeltAvvik)
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi har gjort avviksvurdering tidligere og det nye beregningsgrunnlaget er likt`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))
        val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 600000.0))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat)
    }

    @Test
    fun `gjør ikke ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag bare er litt forskjellig`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))

        val resultat1 = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 600000.1))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat1)

        val resultat2 = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 599999.999999994))
        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat2)
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og beregningsgrunnlag er forskjellig`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))

        val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 500000.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ny avviksvurdering når vi har avviksvurdering fra før og mottar beregningsgrunnlag med beløp 0 kr`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))

        val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 0.0))
        assertIs<Avviksvurderingsresultat.AvvikVurdert>(resultat)
    }

    @Test
    fun `gjør ny avviksvurdering når vi mottar sammenligningsgrunnlag`() {
        val avviksvurderinger = avviksvurderinger()
        val avviksvurdering = avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))

        assertTrue(avviksvurdering.vurdering.harAkseptabeltAvvik)
    }

    @Test
    fun `nytt avviksvurderingsgrunnlag blir lagt til i historikken når vi mottar nytt beregningsgrunnlag som utløser ny avviksvurdering`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))
        avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 500000.0)) // Trigger ny avviksvurdering
        val resultat = avviksvurderinger.nyttBeregningsgrunnlag(beregningsgrunnlag(arbeidsgiver to 500000.0)) // Skal ikke trigge ny avviksvurdering fordi den har likt beregningsgrunnlag som linja over

        assertIs<Avviksvurderingsresultat.TrengerIkkeNyVurdering>(resultat)
        assertEquals(2, avviksvurderinger.grunnlagene().size)
    }

    @Test
    fun `nytt avviksvurderingsgrunnlag blir lagt til i historikken når vi mottar sammenligningsgrunnlag`() {
        val avviksvurderinger = avviksvurderinger()
        avviksvurderinger.nyttSammenligningsgrunnlag(sammenligningsgrunnlag(1.januar, arbeidsgiver to 600000.0), beregningsgrunnlag(arbeidsgiver to 600000.0))

        assertEquals(1, avviksvurderinger.grunnlagene().size)
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
