CREATE TABLE vilkårsgrunnlag_kobling
(
    løpenummer         BIGSERIAL NOT NULL PRIMARY KEY,
    vilkårsgrunnlag_id UUID      NOT NULL,
    avviksvurdering_id UUID      NOT NULL,
    fødselsnummer      VARCHAR   NOT NULL,
    opprettet          TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (fødselsnummer, vilkårsgrunnlag_id)
);