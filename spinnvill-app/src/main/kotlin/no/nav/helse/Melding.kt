package no.nav.helse

sealed class Melding(
    val innhold: Map<String, Any>
) {
    class Behov(
        val behov: Set<String>,
        innhold: Map<String, Any>
    ) : Melding(innhold)

    class Hendelse(
        val navn: String,
        innhold: Map<String, Any>
    ) : Melding(innhold)

    class Løsning(
        innhold: Map<String, Any>
    ) : Melding(innhold)
}

