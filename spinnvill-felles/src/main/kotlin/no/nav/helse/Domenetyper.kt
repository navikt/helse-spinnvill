package no.nav.helse

@JvmInline
value class Fødselsnummer(val value: String)

@JvmInline
value class Arbeidsgiverreferanse(val value: String)

@JvmInline
value class InntektPerMåned(val value: Double)

@JvmInline
value class Fordel(val value: String)

@JvmInline
value class Beskrivelse(val value: String)

@JvmInline
value class OmregnetÅrsinntekt(val value: Double)

fun String.somFnr() = Fødselsnummer(this)
fun String.somArbeidsgiverref() = Arbeidsgiverreferanse(this)
