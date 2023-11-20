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
    
    start --> utkast --> avviksvurdering_finnes
    avviksvurdering_finnes -- ja --> avviksvurdering_gjort
    avviksvurdering_finnes -- nei --> hent_sammenligningsgrunnlag --> ny_avviksvurdering --> utkast
    avviksvurdering_gjort -- ja --> ikke_gjør_avviksvurdering
    avviksvurdering_gjort -- nei --> gjør_avviksvurdering
```

### Datamodell
```mermaid
---
title: Avviksvurdering
---
erDiagram
    AVVIKSVURDERING ||--|{ INNRAPPORTERT_INNTEKT: har
    AVVIKSVURDERING ||--|{ OMMREGNET_AARSINNTEKT: har
    OMMREGNET_AARSINNTEKT }|--|| ARBEIDSGIVER: har
    OMMREGNET_AARSINNTEKT }|--|| INNTEKT: har
    
    AVVIKSVURDERING {
        string foedselsnummer PK
        date skjaeringstidspunkt PK
        timestamp opprettet
    }
    
    ARBEIDSGIVER {
        string organisasjonsnummer
    }

    INNTEKT {
        float inntekt
    }

    INNRAPPORTERT_INNTEKT {
        string organisasjonsnummer
        float[] inntekter
    }
```

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).