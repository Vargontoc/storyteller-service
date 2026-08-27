package es.vargontoc.storyteller.domain.enums;

import es.vargontoc.storyteller.domain.model.AudioToneParams;

public enum VoiceTonePreset {
    
    CALM(0.45, 0.5, 0.8),
    ADVENTURE(0.85, 0.4, 0.9),
    INTENSE(0.3, 0.6, 0.6),
    NEUTRO(0.5, 0.5, 0.8),
    CUSTOM(null, null, null);

    private final Double exageration;
    private final Double cfgWeight;
    private final Double temperature;

    VoiceTonePreset(Double exageration, Double cfgWeight, Double temperature) {
        this.exageration = exageration;
        this.cfgWeight = cfgWeight;
        this.temperature = temperature;
    }

    public AudioToneParams toParams() {
        if(this == CUSTOM)
            throw new IllegalStateException("CUSTOM no tiene params fijos, usa los del comando");
        return new AudioToneParams(exageration, cfgWeight, temperature);
    }
}
