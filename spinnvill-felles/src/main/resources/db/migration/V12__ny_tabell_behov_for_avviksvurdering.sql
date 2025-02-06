CREATE table avviksvurdering_behov
(
    behov_id           uuid PRIMARY KEY,
    fødselsnummer      VARCHAR(11) NOT NULL,
    skjæringstidspunkt DATE        NOT NULL,
    opprettet          TIMESTAMP   NOT NULL,
    løst               TIMESTAMP,
    json               json        NOT NULL
);
