ALTER TABLE beregningsgrunnlag
    ADD CONSTRAINT unikt_beregningsgrunnlag UNIQUE (avviksvurdering_ref, organisasjonsnummer, inntekt)