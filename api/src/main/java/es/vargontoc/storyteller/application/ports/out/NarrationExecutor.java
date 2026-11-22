package es.vargontoc.storyteller.application.ports.out;

/**
 * Punto de extension de la ejecucion de la narracion (TASK-021). StoryNarrationService lo invoca una vez
 * concedido el turno y marcado el cuento como narrandose. Quien lo implemente es responsable de liberar el
 * turno de GenerationGate al terminar, con exito o con error.
 */
public interface NarrationExecutor {

    /** Lanza la narracion del cuento con la voz elegida (nombre y audio de referencia ya tomados). */
    void execute(Long storyId, String voiceName, byte[] voiceSample);
}
