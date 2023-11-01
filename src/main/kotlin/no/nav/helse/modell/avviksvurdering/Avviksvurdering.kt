package no.nav.helse.modell.avviksvurdering

class Avviksvurdering(
    private val omregnedeÅrsinntekter: OmregnedeÅrsinntekter,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag
) {

    internal fun avviksvurderingGjortFor(omregnedeÅrsinntekter: OmregnedeÅrsinntekter) =
        this.omregnedeÅrsinntekter == omregnedeÅrsinntekter


}