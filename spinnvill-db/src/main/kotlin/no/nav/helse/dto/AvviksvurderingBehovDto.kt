package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AvviksvurderingBehovDto(
    val id: UUID,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val løst: LocalDateTime?,
    val json: Map<String, Any>
)