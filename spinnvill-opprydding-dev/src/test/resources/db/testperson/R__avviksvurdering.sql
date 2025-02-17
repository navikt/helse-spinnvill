INSERT INTO avviksvurdering(id, fødselsnummer, skjæringstidspunkt, opprettet) VALUES ('${avviksvurdering_id}', ${fødselsnummer}, '${skjæringstidspunkt}', now());
INSERT INTO avviksvurdering_behov(behov_id, fødselsnummer, skjæringstidspunkt, opprettet, løst, json) VALUES ('${behov_id}', ${fødselsnummer}, '${skjæringstidspunkt}', now(), now(), '{}'::json);
INSERT INTO sammenligningsgrunnlag(id, avviksvurdering_ref, arbeidsgiverreferanse) VALUES ('${sammenligninsgrunnlag_id}', '${avviksvurdering_id}', '987654321');
INSERT INTO beregningsgrunnlag(id, avviksvurdering_ref, organisasjonsnummer, inntekt) VALUES (gen_random_uuid(), '${avviksvurdering_id}', '987654321', 600000.0);
INSERT INTO manedsinntekt(id, sammenligningsgrunnlag_ref, inntekt, år, måned, beskrivelse, fordel, inntektstype) VALUES (gen_random_uuid(), '${sammenligninsgrunnlag_id}', 50000.0, 2018, 1, 'beskrivelse', 'fordel', 'LOENNSINNTEKT');
INSERT INTO manedsinntekt(id, sammenligningsgrunnlag_ref, inntekt, år, måned, beskrivelse, fordel, inntektstype) VALUES (gen_random_uuid(), '${sammenligninsgrunnlag_id}', 50000.0, 2018, 2, 'beskrivelse', 'fordel', 'LOENNSINNTEKT');
