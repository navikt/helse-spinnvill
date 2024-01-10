delete from beregningsgrunnlag a
    using beregningsgrunnlag b
    where a.id > b.id and a.avviksvurdering_ref = b.avviksvurdering_ref and a.organisasjonsnummer = b.organisasjonsnummer and a.inntekt::int = b.inntekt::int;

alter table beregningsgrunnlag drop constraint unikt_beregningsgrunnlag;

alter table beregningsgrunnlag add constraint unikt_beregningsgrunnlag unique (avviksvurdering_ref, organisasjonsnummer);
