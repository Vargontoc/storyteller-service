package es.vargontoc.storyteller.application.ports.out;

public interface PortraitCandidateAssetStorage {
    String saveCandidateAsset(Long catalogCharacterId, byte[] content);
    void deleteCandidateAsset(String assetPath);
}
