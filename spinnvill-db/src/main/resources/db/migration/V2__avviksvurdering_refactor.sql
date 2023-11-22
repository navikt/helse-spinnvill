DROP TABLE avviksvurdering;

CREATE TABLE AVVIKSVURDERING
(
    id                 UUID PRIMARY KEY,
    fødselsnummer      VARCHAR(11) NOT NULL,
    skjæringstidspunkt DATE        NOT NULL,
    opprettet          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE BEREGNINGSGRUNNLAG
(
    id                  UUID PRIMARY KEY,
    avviksvurdering_ref UUID REFERENCES AVVIKSVURDERING (id) NOT NULL,
    organisasjonsnummer VARCHAR(9),
    inntekt             DECIMAL                              NOT NULL
);

CREATE TABLE SAMMENLIGNINGSGRUNNLAG
(
    id                  UUID PRIMARY KEY,
    avviksvurdering_ref UUID REFERENCES AVVIKSVURDERING (id) NOT NULL,
    organisasjonsnummer VARCHAR(9)
);

CREATE TABLE MANEDSINNTEKT
(
    id                         UUID PRIMARY KEY,
    sammenligningsgrunnlag_ref UUID REFERENCES SAMMENLIGNINGSGRUNNLAG (id) NOT NULL,
    inntekt                    DECIMAL                                     NOT NULL,
    år                         INT                                         NOT NULL,
    måned                      INT                                         NOT NULL
)