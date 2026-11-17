package es.vargontoc.storyteller.application.ports.out.persistence;

public interface CatalogCharacterUsagePort {
    boolean isUsedByStorySnapshot(Long catalogCharacterId);
}
