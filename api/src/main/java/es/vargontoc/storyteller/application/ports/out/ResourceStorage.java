package es.vargontoc.storyteller.application.ports.out;

import es.vargontoc.storyteller.domain.enums.KindImage;

public interface ResourceStorage {

    String saveImage(Long storyId, KindImage kind, Long id, byte[] imageBytes);

    String saveAudio(Long storyId, Long id, byte[] audioBytes);

    byte[] getResource(String path);
}