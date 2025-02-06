package no.nav.helse.db

import no.nav.helse.Fødselsnummer
import no.nav.helse.dto.AvviksvurderingBehovDto
import no.nav.helse.helpers.februar
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class AvviksvurderingBehovTest {

    private val database = TestDatabase.database()

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
    }

    @Test
    fun `Kan lagre et avviksvurderingBehov`() {
        val etAvviksvurderingBehov = etAvviksvurderingBehov()
        val lagretAvviksvurderingBehov = database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        assertNotNull(lagretAvviksvurderingBehov.id)
        assertEquals(etAvviksvurderingBehov.id, lagretAvviksvurderingBehov.id)
        assertEquals(etAvviksvurderingBehov.fødselsnummer, lagretAvviksvurderingBehov.fødselsnummer)
        assertEquals(etAvviksvurderingBehov.skjæringstidspunkt, lagretAvviksvurderingBehov.skjæringstidspunkt)
        assertEquals(etAvviksvurderingBehov.opprettet, lagretAvviksvurderingBehov.opprettet)
    }

    @Test
    fun `Finner ikke et løst avviksvurderingBehov`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, løst = LocalDateTime.now())
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)

        assertNull(funnetAvviksvurderingBehov)
    }

    @Test
    fun `Finner kun uløst avviksvurderingBehov`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val id = UUID.randomUUID()
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, løst = LocalDateTime.now(), id = UUID.randomUUID())
        val etTilAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, løst = null, id = id)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)
        database.lagreAvviksvurderingBehov(etTilAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertEquals(id, funnetAvviksvurderingBehov?.id)
    }

    @Test
    fun `Finner flere uløste avviksvurderingBehov på samme fødselsnummer men forskjellig skjæringstidspunkt`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val etTilSkjæringstidspunkt = 1.februar
        val id = UUID.randomUUID()
        val enTilId = UUID.randomUUID()
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, løst = null, id = id)
        val etTilAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= etTilSkjæringstidspunkt, løst = null, id = enTilId)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)
        database.lagreAvviksvurderingBehov(etTilAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)
        val etTilfunnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), etTilSkjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertNotNull(etTilfunnetAvviksvurderingBehov)
        assertEquals(id, funnetAvviksvurderingBehov?.id)
        assertEquals(enTilId, etTilfunnetAvviksvurderingBehov?.id)
    }

    @Test
    fun `Finner et ubehandlet avviksvurderingBehov`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer, skjæringstidspunkt)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertEquals(etAvviksvurderingBehov.id, funnetAvviksvurderingBehov?.id)
        assertEquals(etAvviksvurderingBehov.fødselsnummer, funnetAvviksvurderingBehov?.fødselsnummer)
        assertEquals(etAvviksvurderingBehov.skjæringstidspunkt, funnetAvviksvurderingBehov?.skjæringstidspunkt)
        assertEquals(etAvviksvurderingBehov.opprettet, funnetAvviksvurderingBehov?.opprettet)
    }

    private fun etAvviksvurderingBehov(
        fødselsnummer: String = "12345678910",
        skjæringstidspunkt: LocalDate = 1.januar,
        id: UUID = UUID.randomUUID(),
        løst: LocalDateTime? = null
    ): AvviksvurderingBehovDto {
        return AvviksvurderingBehovDto(
            id = id,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = LocalDateTime.now(),
            løst = løst,
            json = emptyMap()
        )
    }
}
