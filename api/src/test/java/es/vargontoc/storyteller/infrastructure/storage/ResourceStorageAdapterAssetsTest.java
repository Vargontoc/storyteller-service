package es.vargontoc.storyteller.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** TASK-010: la retirada de imágenes borra de verdad los ficheros, no solo la referencia. */
class ResourceStorageAdapterAssetsTest {

    /** Id de cuento propio de la prueba para no tocar datos reales del directorio de trabajo. */
    private static final long STORY = 987_654_321L;

    private final ResourceStorageAdapter storage = new ResourceStorageAdapter();
    private final Path storyDir = Path.of("stories", String.valueOf(STORY));

    @AfterEach
    void cleanUp() throws IOException {
        if (Files.exists(storyDir))
            try (var paths = Files.walk(storyDir)) {
                for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator)
                    Files.deleteIfExists(p);
            }
    }

    @Test
    void deleteImportedImageRemovesTheFileButKeepsOtherAssetsOfThePage() throws IOException {
        String imported = storage.saveImportedImage(STORY, 1L, new byte[] { 1, 2, 3 });
        String audio = storage.saveAudio(STORY, 1L, new byte[] { 9 });

        storage.deleteImportedImage(STORY, 1L);

        assertFalse(Files.exists(Path.of(imported)));
        assertTrue(Files.exists(Path.of(audio)));
    }

    @Test
    void deleteImportedImageOfAPageWithoutImageDoesNothing() {
        storage.deleteImportedImage(STORY, 2L);
    }

    @Test
    void deletePageAssetsRemovesNonEmptyDirectoriesOfThePageAndItsDescendants() throws IOException {
        String cover = storage.saveImportedImage(STORY, 10L, new byte[] { 1 });
        String page = storage.saveImportedImage(STORY, 11L, new byte[] { 2 });
        String descendant = storage.saveImportedImage(STORY, 12L, new byte[] { 3 });
        String other = storage.saveImportedImage(STORY, 13L, new byte[] { 4 });
        storage.saveAudio(STORY, 11L, new byte[] { 5 });

        storage.deletePageAssets(STORY, 11L, List.of(12L));

        assertFalse(Files.exists(Path.of(page)));
        assertFalse(Files.exists(Path.of(descendant)));
        assertFalse(Files.exists(storyDir.resolve("pages").resolve("11")));
        assertFalse(Files.exists(storyDir.resolve("pages").resolve("12")));
        // Las demás páginas no se tocan
        assertEquals(1, Files.readAllBytes(Path.of(cover)).length);
        assertEquals(1, Files.readAllBytes(Path.of(other)).length);
    }
}
