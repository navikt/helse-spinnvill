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
### Konsept
```mermaid
---
title: Prosessflyt for avviksvurdering
---
flowchart TD
    start(Melding om utkast til vedtak)
    utkast(Håndter utkast til vedtak)
    avviksvurdering_finnes{{Finnes avviksvurdering?}}
    ny_avviksvurdering(Opprett ufullstendig avviksvurdering)
    avviksvurdering_gjort{{Er avviksvurdering gjort med <br> beregningsgrunnlag fra utkast til vedtak?}}
    hent_sammenligningsgrunnlag(Hent sammenligningsgrunnlag)
    gjør_avviksvurdering(Gjør avviksvurdering)
    ikke_gjør_avviksvurdering(Ikke gjør en ny avviksvurdering)
    har_ufulstendig_avviksvurdering{{Har ufullstendig avviksvurdering?}}
    gjennbruk_avviksvurdering(Gjennbruk ufullstendig avviksvurdering)
    opprett_avviksvurdering(Opprett ny avviksvurdering)
    
    start --> utkast --> avviksvurdering_finnes
    avviksvurdering_finnes -- ja --> avviksvurdering_gjort
    avviksvurdering_finnes -- nei --> hent_sammenligningsgrunnlag --> ny_avviksvurdering --> utkast
    avviksvurdering_gjort -- ja --> ikke_gjør_avviksvurdering
    avviksvurdering_gjort -- nei --> har_ufulstendig_avviksvurdering
    har_ufulstendig_avviksvurdering -- ja --> gjennbruk_avviksvurdering
    har_ufulstendig_avviksvurdering -- nei --> opprett_avviksvurdering
    gjennbruk_avviksvurdering & opprett_avviksvurdering --> gjør_avviksvurdering
```

### Datamodell
```mermaid
---
title: Avviksvurdering
---
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
      string organisasjonsnummer
      float aarsinntekt
    }
```

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).