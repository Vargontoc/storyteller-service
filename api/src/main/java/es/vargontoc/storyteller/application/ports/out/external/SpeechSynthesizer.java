package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.request.SpeechSynthesisRequest;

public interface SpeechSynthesizer {

    /**
     * Sintetiza un fragmento y devuelve el audio WAV.
     *
     * @throws es.vargontoc.storyteller.shared.exceptions.AppException si el motor falla, agota el tiempo o responde vacío
     */
    byte[] synthesize(SpeechSynthesisRequest request);
}
