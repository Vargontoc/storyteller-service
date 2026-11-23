package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.AudioFragmentKind;

public record AudioFragment(String text, double exaggeration, double cfgWeight, double temperature,
        AudioFragmentKind kind) {

    public AudioFragment {
        if (kind == null)
            kind = AudioFragmentKind.NARRATION;
    }

    public AudioFragment(String text, double exaggeration, double cfgWeight, double temperature) {
        this(text, exaggeration, cfgWeight, temperature, AudioFragmentKind.NARRATION);
    }
}
