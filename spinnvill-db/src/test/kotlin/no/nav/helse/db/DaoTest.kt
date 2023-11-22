package no.nav.helse.db

import no.nav.helse.TestDatabase
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class DaoTest {

    private val dao = Dao(TestDatabase.database().datasource())

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
    }
    @Test
    fun `finner ikke avviksvurdering hvis ikke det finnes noen`() {
        val avviksvurderingJson = dao.finnAvviksvurdering("12345678911", 1.januar)
        assertEquals(null, avviksvurderingJson)
    }

    @Test
    fun `kan opprette avviksvurdering`() {
        dao.opprettAvviksvurdering("12345678910", 1.januar, "{}")
        val avviksvurderingJson = dao.finnAvviksvurdering("12345678910", 1.januar)
        assertEquals("{}", avviksvurderingJson)
    }
}