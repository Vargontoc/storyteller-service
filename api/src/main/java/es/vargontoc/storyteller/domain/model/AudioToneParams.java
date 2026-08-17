package es.vargontoc.storyteller.domain.model;

public record  AudioToneParams(double exageration, double cfgWeight, double temperature) {
    public AudioToneParams {
        if(exageration <= 0.25)
            exageration = 0.25;
        if(exageration >= 2.00)
            exageration = 2.00;

        if(cfgWeight <= 0.0)
            cfgWeight = 0.0;
        if(cfgWeight >= 1.0)
            cfgWeight = 1.0;

        if(temperature <= 0.05)
            temperature = 0.05;
        if(temperature >= 5.0)
            temperature = 5.0;
    }
}
