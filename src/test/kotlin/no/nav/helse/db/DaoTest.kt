package no.nav.helse.db

import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DaoTest {

    private val dao = Dao(TestDatabase.dataSource())
    @Test
    fun `finner ikke avviksvurdering hvis ikke det finnes noen`() {
        val avviksvurderingJson = dao.finnAvviksvurdering("12345678910", 1.januar)
        assertEquals(null, avviksvurderingJson)
    }
}