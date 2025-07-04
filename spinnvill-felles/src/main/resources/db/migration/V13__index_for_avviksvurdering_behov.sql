create index if not exists uløste_avviksvurderingbehov_idx on avviksvurdering_behov (fødselsnummer, skjæringstidspunkt) where løst is null;
