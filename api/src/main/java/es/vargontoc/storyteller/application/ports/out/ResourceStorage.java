package es.vargontoc.storyteller.application.ports.out;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.KindImage;

public interface ResourceStorage {

    String saveImage(Long storyId, KindImage kind, Long id, byte[] imageBytes);

    String saveAudio(Long storyId, Long id, byte[] audioBytes);

    byte[] getResource(String path);

    void deleteStoryAssets(long idStory);

    void deleteCharacterAssets(long idStory, long characterId);

    void deletePageAssets(long idStory, long pageId, List<Long> descendants);
}