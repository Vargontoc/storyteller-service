package es.vargontoc.storyteller.application.ports.out;

import java.io.File;
import java.util.List;

import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.request.PageAssetRequest;

public interface ResourceStorage {

    String saveImage(Long storyId, KindImage kind, Long id, byte[] imageBytes);

    boolean existsAsset(String asset);

    String saveAudio(Long storyId, Long id, byte[] audioBytes);

    byte[] getResource(String path);

    void deleteStoryAssets(long idStory);

    void deleteCharacterAssets(long idStory, long characterId);

    void deletePageAssets(long idStory, long pageId, List<Long> descendants);

    File downloadStory(String json, List<PageAssetRequest> assets);
}