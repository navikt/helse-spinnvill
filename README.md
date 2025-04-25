# Spinnvill
[![Bygg og deploy Spinnvill](https://github.com/navikt/helse-spinnvill/actions/workflows/main.yml/badge.svg)](https://github.com/navikt/helse-spinnvill/actions/workflows/main.yml)

## Beskrivelse
Backend som vurderer automatisk behandling av sykepengesøknader

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## Format på Commit-meldinger 
I dette repoet skal det commites på dette formatet:
- `[gitmoji] [teksten din]`

  gitmoji finner du her: (https://gitmoji.dev/)
- Eksempel: ✅ Test automatisering uten varsel

NB: Husk å bytte til å bruke unicode characters i stedet for `:<emote>:` notasjon

## Avviksvurdering
### Beslutningstre for avviksvurdering
```mermaid
flowchart TD
    start((Nytt 
    avviksvurdering-
    behov))
    finnes_ubesvart_behov{Finnes et 
    ubesvart behov 
    for fnr og 
    skjæringstidspunkt?}
    ubesvart_behov_finnes(Ignorer nytt behov)
    finnes_tidligere_avviksvurdering{Lagre behov
    Er det 
    avviksvurdert 
    tidligere?
    }
    hent_sammenligningsgrunnlag(Hent sammenligningsgrunnlag)
    finn_riktig_avviksvurdering(Finn avviksvurdering med riktig ID)
    forskjellige_beregningsgrunnlag{
    Er
beregningsgrunnlagene
forskjellige?
}
    gjør_avviksvurdering(Gjør avviksvurdering)
    ikke_gjør_avviksvurdering(Ikke gjør en ny avviksvurdering)
    
    start --> finnes_ubesvart_behov
    finnes_ubesvart_behov -- ja --> ubesvart_behov_finnes
    finnes_ubesvart_behov -- nei --> finnes_tidligere_avviksvurdering
    finnes_tidligere_avviksvurdering -- nei --> hent_sammenligningsgrunnlag
    finnes_tidligere_avviksvurdering -- ja --> forskjellige_beregningsgrunnlag
    forskjellige_beregningsgrunnlag -- nei --> ikke_gjør_avviksvurdering
    forskjellige_beregningsgrunnlag -- ja --> gjør_avviksvurdering
    hent_sammenligningsgrunnlag -. Løsning på behov .-> finn_riktig_avviksvurdering --> gjør_avviksvurdering
```

### Kommunikasjonsflyt for avviksvurdering
```mermaid
sequenceDiagram
  participant Spes as Spesialist
  participant Spinn as Spinnvill
  participant Kafka
  participant Db as DB
  
  Spes ->> Spinn: Avviksvurdering-behov
  alt har ubesvart behov
  Spinn ->> Spinn: ignorer nytt behov
  else har ikke ubesvart behov
    Spinn ->> Db: lagre nytt behov
    alt har gjort avviksvurdering før
      alt har forskjellig beregningsgrunnlag
        Spinn ->> Spinn: Vurder avvik
        Spinn ->> Spes: Løs behov: ny avviksvurdering foretatt
      else har ikke forskjellig beregningsgrunnlag
       Spinn ->> Spes: Løs behov: ny avviksvurdering ikke foretatt
      end
    else har ikke gjort avviksvurdering før
      Spinn -->> Kafka: Hent sammenligningsgrunnlag
      Kafka -->> Spinn: Sammenligningsgrunnlag
      Spinn ->> Db: Hent behov for gitt ID
      Db ->> Spinn: Avviksvurdering-behov
      Spinn ->> Spinn: Vurder avvik
      Spinn ->> Spes: Løs behov: ny avviksvurdering foretatt
    end
  end
```

### Datamodell for avviksvurdering
```mermaid
erDiagram
    AVVIKSVURDERING ||--o{ BEREGNINGSGRUNNLAG: har
    AVVIKSVURDERING ||--|{ SAMMENLIGNINGSGRUNNLAG: har
    SAMMENLIGNINGSGRUNNLAG ||--|{ MANEDSINNTEKT: har
  
    AVVIKSVURDERING {
      string foedselsnummer PK
      date skjaeringstidspunkt PK
      timestamp opprettet
    }

    SAMMENLIGNINGSGRUNNLAG {
      string arbeidsgiverreferanse
    }
    
    MANEDSINNTEKT {
        float inntekt
        int year
        int month
        string fordel
        string beskrivelse
        enum inntektstype
    }
  
    BEREGNINGSGRUNNLAG {
      string arbeidsgiverreferanse
      float aarsinntekt
    }
```

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
