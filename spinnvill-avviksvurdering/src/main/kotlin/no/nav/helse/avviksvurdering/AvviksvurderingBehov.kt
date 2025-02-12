package no.nav.helse.avviksvurdering

import no.nav.helse.Fødselsnummer
import java.time.LocalDate
import java.util.*

class AvviksvurderingBehov private constructor(
    val vilkårsgrunnlagId: UUID,
    val behovId: UUID,
    val skjæringstidspunkt: LocalDate,
    val fødselsnummer: Fødselsnummer,
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val beregningsgrunnlag : Beregningsgrunnlag,
    val json: Map<String, Any>,
    private var løst: Boolean
) {
    fun erLøst() = løst

    fun løs() {
        løst = true
    }

    companion object {
        fun nyttBehov(
            vilkårsgrunnlagId: UUID,
            behovId: UUID,
            skjæringstidspunkt: LocalDate,
            fødselsnummer: Fødselsnummer,
            vedtaksperiodeId: UUID,
            organisasjonsnummer: String,
            beregningsgrunnlag: Beregningsgrunnlag,
            json: Map<String, Any>,
        ): AvviksvurderingBehov {
            return AvviksvurderingBehov(
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                behovId = behovId,
                skjæringstidspunkt = skjæringstidspunkt,
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = organisasjonsnummer,
                beregningsgrunnlag = beregningsgrunnlag,
                json = json,
                løst = false
            )
        }
    }
}