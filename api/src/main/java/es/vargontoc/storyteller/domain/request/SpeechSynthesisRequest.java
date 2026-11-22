package es.vargontoc.storyteller.domain.request;

/**
 * Petición de síntesis de un fragmento en el flujo de narración con escaleta: texto, idioma,
 * audio de referencia de la voz (bytes) y expresividad.
 */
public record SpeechSynthesisRequest(
    String text,
    String language,
    byte[] referenceSample,
    double exaggeration,
    double cfgWeight,
    double temperature
) {
    public SpeechSynthesisRequest {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("El texto del fragmento es obligatorio");
        if (language == null || language.isBlank())
            throw new IllegalArgumentException("El idioma es obligatorio");
        if (referenceSample == null || referenceSample.length == 0)
            throw new IllegalArgumentException("El audio de referencia es obligatorio");
    }
}
