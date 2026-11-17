package es.vargontoc.storyteller.application.ports.out;

public interface PortraitGenerationTaskRegistry {
    void register(Long generationJobId);
    void unregister(Long generationJobId);
    boolean isAlive(Long generationJobId);
}
