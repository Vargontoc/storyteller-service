package es.vargontoc.storyteller.application.ports.out.external;

import java.util.List;

import es.vargontoc.storyteller.domain.model.PageAudioScript;

/** Puerto del agente narrador de audio: divide el texto aceptado en fragmentos y anota su expresividad. */
public interface NarratorAgent {

    /**
     * Guion de audio de las páginas interiores.
     *
     * @param pageTexts texto aceptado de cada página interior, en orden (la primera es la página 1; sin portada)
     * @return un guion por página recibida, en el mismo orden
     * @throws es.vargontoc.storyteller.shared.exceptions.AppException si el modelo no responde, falla o
     *         devuelve un guion incompleto o malformado
     */
    List<PageAudioScript> narrate(List<String> pageTexts);
}
