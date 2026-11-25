package es.vargontoc.storyteller.application.ports.out;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.KindImage;

public interface ResourceStorage {

    String saveImage(Long storyId, KindImage kind, Long id, byte[] imageBytes);

    boolean existsAsset(String asset);

    /** Guarda tal cual (sin convertir) la imagen importada de una portada o página y borra la importada anterior. Devuelve su ruta. */
    String saveImportedImage(long storyId, long pageId, byte[] imageBytes);

    /** Borra del almacén la imagen importada de una portada o página (el fichero, no la referencia). */
    void deleteImportedImage(long storyId, long pageId);

    String saveAudio(Long storyId, Long id, byte[] audioBytes);

    byte[] getResource(String path);

    /** Guarda el audio narrado de una pagina interior y devuelve su ruta. */
    String saveNarrationAudio(long storyId, int page, byte[] audioBytes);

    /** Elimina todos los ficheros de audio de la narracion del cuento. */
    void deleteNarrationAudio(long storyId);

    void saveVoiceSample(long voiceId, byte[] sampleBytes);

    /** Devuelve null si no existe la muestra. */
    byte[] getVoiceSample(long voiceId);

    void deleteVoiceSample(long voiceId);

    void deleteStoryAssets(long idStory);

    void deleteCharacterAssets(long idStory, long characterId);

    void deletePageAssets(long idStory, long pageId, List<Long> descendants);
}