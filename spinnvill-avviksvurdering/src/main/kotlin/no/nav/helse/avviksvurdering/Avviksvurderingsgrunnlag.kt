package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import no.nav.helse.avviksvurdering.Avviksvurderingsresultat.AvvikVurdert
import no.nav.helse.avviksvurdering.Avviksvurderingsresultat.TrengerIkkeNyVurdering
import org.slf4j.LoggerFactory
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

    internal fun vurderAvvik(beregningsgrunnlag: Beregningsgrunnlag): Avviksvurderingsresultat {
        if (kilde == Kilde.INFOTRYGD || this.beregningsgrunnlag.erLikt(beregningsgrunnlag))
            return TrengerIkkeNyVurdering(this)

        this.beregningsgrunnlag = beregningsgrunnlag
        val avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        return AvvikVurdert(
            grunnlag = this,
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

    internal fun vurderAvvik(): AvvikVurdert {
        val beregningsgrunnlag = this.beregningsgrunnlag
        check(beregningsgrunnlag is Beregningsgrunnlag)
        val avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        return AvvikVurdert(
            grunnlag = this,
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

    internal fun trengerNyVurdering(beregningsgrunnlag: Beregningsgrunnlag): Boolean {
        return when {
            this.kilde == Kilde.INFOTRYGD -> false
            this.beregningsgrunnlag is Ingen -> false
            this.beregningsgrunnlag.erLikt(beregningsgrunnlag) -> {
                loggGjenbrukAvAvviksvurderingsgrunnlag(beregningsgrunnlag)
                false
            }

            else -> true
        }
    }

    internal fun harLiktBeregningsgrunnlagSom(beregningsgrunnlag: Beregningsgrunnlag): Boolean {
        return this.beregningsgrunnlag.erLikt(beregningsgrunnlag)
    }

    private fun loggGjenbrukAvAvviksvurderingsgrunnlag(beregningsgrunnlag: Beregningsgrunnlag) {
        val forrigeGrunnlag = this.beregningsgrunnlag
        val nyttGrunnlag = beregningsgrunnlag
        val forrigeÅrsbeløp = finnTotaltOmregnetÅrsinntekt(forrigeGrunnlag)
        val nåværendeÅrsbeløp = finnTotaltOmregnetÅrsinntekt(nyttGrunnlag)
        if (forrigeÅrsbeløp != nåværendeÅrsbeløp) sikkerlogg.warn(
            """
            Inntekter er like nok til at det ikke er nødvendig med ny avviksvurdering. Fnr: ${fødselsnummer.value}, skjæringstidspunkt: $skjæringstidspunkt 
            Forrige beløp: $forrigeÅrsbeløp, nåværende beløp: $nåværendeÅrsbeløp
            """.trimIndent()
        ) else sikkerlogg.warn(
            """
            Inntekter er like, det er ikke nødvendig med ny avviksvurdering. Fnr: ${fødselsnummer.value}, skjæringstidspunkt: $skjæringstidspunkt
            Beløp: $nåværendeÅrsbeløp
            """.trimIndent()
        )
    }

    /*
        Skrevet kun for bruk til logging.

        Denne returerer tulleverdi hvis den ikke finner riktig verdi. Det ikke er ønskelig å gjøre noe større nummer ut av
        det siden dette kun er kode for logging.
     */
    private fun finnTotaltOmregnetÅrsinntekt(beregningsgrunnlag: IBeregningsgrunnlag): Double {
        val beløp: Double? = if (beregningsgrunnlag is Beregningsgrunnlag) beregningsgrunnlag.totalOmregnetÅrsinntekt else null

        return beløp ?: run {
            sikkerlogg.warn("Forventet å finne totalOmregnetÅrsinntekt for $beregningsgrunnlag - bruker tulleverdi")
            13371337.13371337
        }
    }

    internal fun gjenbrukSammenligningsgrunnlag(): Avviksvurderingsgrunnlag {
        return nyttGrunnlag(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
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

        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
