package no.nav.helse

import org.junit.jupiter.api.Test

internal class DaoTest: AbstractDatabaseTest() {

    private val dao = Dao(dataSource)
    private val fødselsnummer = "12345678910"

    @Test
    fun `kan slette person`() {
        opprettPerson(fødselsnummer)
        assertTabellinnhold {
            (it == 1) to "Forventer at hver tabell har én rad"
        }
        dao.slett(fødselsnummer.somFnr())
        assertTabellinnhold {
            (it == 0) to "Forventer at alle tabeller er tomme"
        }
    }

    @Test
    fun `sletter kun aktuell person`() {
        val annetFødselsnummer = "10987654321"
        opprettPerson(fødselsnummer)
        opprettPerson(annetFødselsnummer)
        assertTabellinnhold {
            (it == 2) to "Forventer at hver tabell har minst to rader"
        }
        dao.slett(fødselsnummer.somFnr())
        assertTabellinnhold {
            (it == 1) to "Forventer at det finnes rader for $annetFødselsnummer"
        }
    }

    @Test
    fun `sletter person med flere avviksvurderinger`() {
        opprettPerson(fødselsnummer)
        opprettPerson(fødselsnummer)
        assertTabellinnhold {
            (it == 2) to "Forventer at hver tabell har to rader"
        }
        dao.slett(fødselsnummer.somFnr())
        assertTabellinnhold {
            (it == 0) to "Forventer at alle tabeller er tomme"
        }
    }
}