package es.vargontoc.storyteller.application.ports.in.character;

public interface PortraitSelectionUseCase {
    void select(Long catalogCharacterId, Long candidateId);
}
