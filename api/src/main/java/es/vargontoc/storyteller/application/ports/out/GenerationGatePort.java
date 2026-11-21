package es.vargontoc.storyteller.application.ports.out;

/**
 * Puerta única de generación: solo una generación (modelo o ComfyUI) a la vez en toda la aplicación.
 */
public interface GenerationGatePort {

    enum GenerationType {
        WRITER, DIRECTOR, CHARACTER_AGENT, PORTRAITS
    }

    /** Pide el turno. Devuelve false si ya hay otra generación en curso. storyId puede ser null. */
    boolean tryAcquire(GenerationType type, Long storyId);

    /** Libera el turno. Sin efecto si la puerta ya está libre. */
    void release();

    /** Indica si la generación en curso es del cuento dado. */
    boolean isGeneratingStory(Long storyId);
}
