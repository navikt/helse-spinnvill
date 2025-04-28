package no.nav.helse.helpers

import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 319999_99999).toString().padStart(11, '0')

fun lagOrganisasjonsnummer() = nextLong(from = 800_000_000, until = 999_999_999).toString()
