package no.nav.helse.avviksvurdering

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.KriterieObserver
import no.nav.helse.OmregnetÅrsinntekt
import org.slf4j.LoggerFactory
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
    private val kilde: Kilde,
) {
    private var avviksprosent: Avviksprosent = Avviksprosent.INGEN
    private val MAKSIMALT_TILLATT_AVVIK = Avviksprosent(25.0)

    private val observers = mutableListOf<KriterieObserver>()

    fun register(vararg observers: KriterieObserver) {
        this.observers.addAll(observers)
    }

    fun håndter(beregningsgrunnlag: Beregningsgrunnlag) {
        if (this.beregningsgrunnlag.erLikt(beregningsgrunnlag)) return
        this.beregningsgrunnlag = beregningsgrunnlag
        avviksprosent = sammenligningsgrunnlag.beregnAvvik(beregningsgrunnlag)
        observers.forEach {
            it.avvikVurdert(
                id = id,
                harAkseptabeltAvvik = avviksprosent <= MAKSIMALT_TILLATT_AVVIK,
                avviksprosent = avviksprosent.avrundetTilFireDesimaler,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                maksimaltTillattAvvik = MAKSIMALT_TILLATT_AVVIK.avrundetTilFireDesimaler
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
            this.beregningsgrunnlag is Ingen -> false
            this.beregningsgrunnlag.erLikt(beregningsgrunnlag) -> {
                loggGjenbrukAvAvviksvurdering(beregningsgrunnlag)
                false
            }

            else -> true
        }
    }

    private fun loggGjenbrukAvAvviksvurdering(beregningsgrunnlag: Beregningsgrunnlag) {
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
        var beløp: Double? = null
        beregningsgrunnlag.accept(object : Visitor {
            override fun visitBeregningsgrunnlag(
                totaltOmregnetÅrsinntekt: Double,
                omregnedeÅrsinntekter: Map<Arbeidsgiverreferanse, OmregnetÅrsinntekt>,
            ) {
                beløp = totaltOmregnetÅrsinntekt
            }
        })

        return beløp ?: run {
            sikkerlogg.warn("Forventet å finne totalOmregnetÅrsinntekt for $beregningsgrunnlag - bruker tulleverdi")
            13371337.13371337
        }
    }

    fun nyAvviksvurdering(): Avviksvurdering {
        return nyAvviksvurdering(fødselsnummer, skjæringstidspunkt, sammenligningsgrunnlag)
    }

    internal companion object {
        internal fun nyAvviksvurdering(
            fødselsnummer: Fødselsnummer,
            skjæringstidspunkt: LocalDate,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
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

        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
