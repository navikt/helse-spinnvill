package no.nav.helse.db

import no.nav.helse.Arbeidsgiverreferanse
import no.nav.helse.Fødselsnummer
import no.nav.helse.OmregnetÅrsinntekt
import no.nav.helse.avviksvurdering.AvviksvurderingBehov
import no.nav.helse.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.helpers.februar
import no.nav.helse.helpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class AvviksvurderingBehovDaoTest {

    private val database = TestDatabase.database()

    @BeforeEach
    fun beforeEach() {
        TestDatabase.reset()
    }

    @Test
    fun `Kan lagre et avviksvurderingBehov`() {
        val etAvviksvurderingBehov = etAvviksvurderingBehov()
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(etAvviksvurderingBehov.fødselsnummer, etAvviksvurderingBehov.skjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertEquals(etAvviksvurderingBehov.behovId, funnetAvviksvurderingBehov?.behovId)
        assertEquals(etAvviksvurderingBehov.fødselsnummer, funnetAvviksvurderingBehov?.fødselsnummer)
        assertEquals(etAvviksvurderingBehov.skjæringstidspunkt, funnetAvviksvurderingBehov?.skjæringstidspunkt)
    }

    @Test
    fun `Finner ikke et løst avviksvurderingBehov når avviksvurdering-behovet er løst`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)
        etAvviksvurderingBehov.løs()
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)

        assertNull(funnetAvviksvurderingBehov)
    }

    @Test
    fun `Finner kun uløst avviksvurderingBehov`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val id = UUID.randomUUID()
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, id = UUID.randomUUID())
        val etTilAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, id = id)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)
        database.lagreAvviksvurderingBehov(etTilAvviksvurderingBehov)
        etAvviksvurderingBehov.løs()
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertEquals(id, funnetAvviksvurderingBehov?.behovId)
    }

    @Test
    fun `Finner flere uløste avviksvurderingBehov på samme fødselsnummer men forskjellig skjæringstidspunkt`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val etTilSkjæringstidspunkt = 1.februar
        val id = UUID.randomUUID()
        val enTilId = UUID.randomUUID()
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= skjæringstidspunkt, id = id)
        val etTilAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer = fødselsnummer, skjæringstidspunkt= etTilSkjæringstidspunkt, id = enTilId)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)
        database.lagreAvviksvurderingBehov(etTilAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)
        val etTilfunnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), etTilSkjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertNotNull(etTilfunnetAvviksvurderingBehov)
        assertEquals(id, funnetAvviksvurderingBehov?.behovId)
        assertEquals(enTilId, etTilfunnetAvviksvurderingBehov?.behovId)
    }

    @Test
    fun `Finner et ubehandlet avviksvurderingBehov`() {
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val etAvviksvurderingBehov = etAvviksvurderingBehov(fødselsnummer, skjæringstidspunkt)
        database.lagreAvviksvurderingBehov(etAvviksvurderingBehov)

        val funnetAvviksvurderingBehov = database.finnUbehandledeAvviksvurderingBehov(Fødselsnummer(fødselsnummer), skjæringstidspunkt)

        assertNotNull(funnetAvviksvurderingBehov)
        assertEquals(etAvviksvurderingBehov.behovId, funnetAvviksvurderingBehov?.behovId)
        assertEquals(etAvviksvurderingBehov.fødselsnummer, funnetAvviksvurderingBehov?.fødselsnummer)
        assertEquals(etAvviksvurderingBehov.skjæringstidspunkt, funnetAvviksvurderingBehov?.skjæringstidspunkt)
    }

    private fun etAvviksvurderingBehov(
        fødselsnummer: String = "12345678910",
        skjæringstidspunkt: LocalDate = 1.januar,
        id: UUID = UUID.randomUUID(),
    ): AvviksvurderingBehov {
        val vilkårsgrunnlagId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val organisasjonsnummer = "00000000"
        val beløp = 200000.0
        return AvviksvurderingBehov.nyttBehov(
            behovId = id,
            fødselsnummer = Fødselsnummer(fødselsnummer),
            skjæringstidspunkt = skjæringstidspunkt,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            beregningsgrunnlag = Beregningsgrunnlag.opprett(
                mapOf(
                    pair = Arbeidsgiverreferanse(organisasjonsnummer) to OmregnetÅrsinntekt(
                        beløp,
                    ),
                ),
            ),
            json = mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "Avviksvurdering" to mapOf(
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId,
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "beløp" to beløp
                        ),
                    )
                )
            ),
        )
    }
}
