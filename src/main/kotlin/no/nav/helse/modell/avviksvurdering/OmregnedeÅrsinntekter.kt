package no.nav.helse.modell.avviksvurdering

class OmregnedeÅrsinntekter(private val inntekter: Map<String, Double>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OmregnedeÅrsinntekter

        return inntekter == other.inntekter
    }

    override fun hashCode(): Int {
        return inntekter.hashCode()
    }
}