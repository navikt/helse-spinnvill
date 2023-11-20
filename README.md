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

## Konsepter
### Avviksvurdering
```mermaid
---
title: Avviksvurdering prosessflyt
---
flowchart TD
    lol[Stiplede bokser er ikke implementert]:::legend
    
    utv(Mottar utkast til vedtak) --> sft{{Sykefraværstilfelle?}}:::ikkeImplementert
    sft --ja--> slgl{{Sammenligningsgrunnlag?}}
    sft --nei--> ny_sft(Opprett sykefraværstilfelle):::ikkeImplementert --> slgl
    
    slgl --ja--> avv{{Finnes avviksvurdering?}}
    slgl --nei--> hent_slgl(Hent sammenligningsgrunnlag) --> lagre_slgl(Lagre sammenligningsgrunnlag):::ikkeImplementert --> utv
    
    avv --ja--> avv_brgl{{Er avviksvurdering gjort med <br> beregningsgrunnlag fra utkast til vedtak}}
    avv --nei--> gjør_avv(Gjør avviksvurdering)
    
    avv_brgl --ja--> ikke_avv(Ikke gjør en ny avviksvurdering)
    avv_brgl --nei--> gjør_avv
    
    classDef ikkeImplementert stroke-dasharray: 5 5 
    classDef legend stroke-dasharray: 5 5, fill:#CBCFD5, color:#23262A, stroke:#23262A
```

## Datamodell
### Avviksvurdering
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