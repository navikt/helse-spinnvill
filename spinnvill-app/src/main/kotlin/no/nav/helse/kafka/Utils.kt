package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID() = UUID.fromString(this.asText())