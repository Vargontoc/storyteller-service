package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.NarrationState;

/** Narracion de un cuento con escaleta: estado y consultas que necesita el servicio de paginas. */
public interface StoryNarrationUseCase {

    /** Estado de narracion calculado desde las paginas interiores actuales. */
    NarrationState getState(Long storyId);

    /** El cuento se esta narrando ahora mismo. */
    boolean isNarrating(Long storyId);

    /** El cuento tiene audio publicado. */
    boolean hasAudio(Long storyId);

    /** Borra a la vez el audio de todas las paginas y la voz guardada. */
    void deleteAudio(Long storyId);

    /**
     * Audio narrado publicado de una pagina interior (pagina >= 1). Portada, pagina sin audio, cuento sin audio
     * o narracion en curso/con error: AppException 404 (no hay audio publicado).
     */
    byte[] getPageAudio(Long storyId, int page);

    /**
     * Inicia la narracion (o renarracion) con la voz elegida; voiceId nulo identifica la voz del sistema.
     * Valida antes de pedir el turno; un rechazo lanza NarrationRejectedException y deja el audio intacto.
     * Devuelve el estado del cuento, ya narrandose.
     */
    NarrationState start(Long storyId, Long voiceId);
}
