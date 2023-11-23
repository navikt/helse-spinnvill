package no.nav.helse.avviksvurdering

class Beregningsgrunnlag(private val omregnedeÅrsinntekter: Map<String, Double>) {

    private val totalOmregnetÅrsinntekt = omregnedeÅrsinntekter.values.sum()

    internal fun beregnAvvik(sammenligningsgrunnlag: Double): Avviksprosent {
        return Avviksprosent.avvik(
            beregningsgrunnlag = totalOmregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beregningsgrunnlag

        return omregnedeÅrsinntekter == other.omregnedeÅrsinntekter
    }

    override fun hashCode(): Int {
        return omregnedeÅrsinntekter.hashCode()
    }

    internal companion object {
        internal val INGEN = Beregningsgrunnlag(emptyMap())
    }
}