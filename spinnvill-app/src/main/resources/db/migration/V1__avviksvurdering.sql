CREATE TABLE avviksvurdering (
    løpenummer BIGSERIAL PRIMARY KEY,
    fødselsnummer VARCHAR NOT NULL,
    skjæringstidspunkt DATE NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    json json NOT NULL
);

CREATE INDEX ON avviksvurdering (fødselsnummer, skjæringstidspunkt);