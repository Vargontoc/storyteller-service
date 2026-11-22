package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.infrastructure.mappers.StoryNarrationMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationJpaRepository;

/** Repositorios JPA y almacen de ficheros simulados en memoria; sin transaccion activa los ficheros se borran al momento. */
class NarrationRepositoryAdapterTest {

    private static final long STORY_ID = 7L;

    private final Map<Long, StoryNarrationJpaEntity> rows = new HashMap<>();
    private final Map<String, byte[]> files = new HashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private boolean failOnSave;

    private final StoryNarrationJpaRepository narrations = mock(StoryNarrationJpaRepository.class);
    private final StoryJpaRepository stories = mock(StoryJpaRepository.class);
    private final ResourceStorage storage = mock(ResourceStorage.class);
    private final NarrationRepositoryAdapter adapter =
            new NarrationRepositoryAdapter(narrations, stories, storage, new StoryNarrationMapper());

    NarrationRepositoryAdapterTest() {
        StoryJpaEntity story = new StoryJpaEntity();
        story.setId(STORY_ID);
        when(stories.findById(STORY_ID)).thenReturn(Optional.of(story));

        when(narrations.findByStoryId(anyLong())).thenAnswer(call -> Optional.ofNullable(rows.get(call.getArgument(0))));
        when(narrations.saveAndFlush(any(StoryNarrationJpaEntity.class))).thenAnswer(call -> {
            if (failOnSave)
                throw new IllegalStateException("fallo al guardar");
            StoryNarrationJpaEntity entity = call.getArgument(0);
            if (entity.getId() == null)
                entity.setId(ids.incrementAndGet());
            rows.put(entity.getStory().getId(), entity);
            return entity;
        });
        doAnswer(call -> {
            StoryNarrationJpaEntity entity = call.getArgument(0);
            rows.remove(entity.getStory().getId());
            return null;
        }).when(narrations).delete(any(StoryNarrationJpaEntity.class));

        when(storage.saveNarrationAudio(anyLong(), anyInt(), any(byte[].class))).thenAnswer(call -> {
            String path = "stories/" + call.getArgument(0) + "/narration/" + call.getArgument(1) + ".wav";
            files.put(path, call.getArgument(2));
            return path;
        });
        when(storage.getResource(anyString())).thenAnswer(call -> files.get(call.<String>getArgument(0)));
        doAnswer(call -> {
            files.keySet().removeIf(p -> p.startsWith("stories/" + call.getArgument(0) + "/narration/"));
            return null;
        }).when(storage).deleteNarrationAudio(anyLong());
    }

    private Map<Integer, byte[]> eightPages() {
        Map<Integer, byte[]> audios = new HashMap<>();
        for (int page = 1; page <= 8; page++)
            audios.put(page, new byte[] {(byte) page});
        return audios;
    }

    @Test
    void markedInProgressIsQueriedAsInProgress() {
        adapter.markInProgress(STORY_ID);

        var narration = adapter.find(STORY_ID).orElseThrow();
        assertEquals(NarrationStatus.IN_PROGRESS, narration.getStatus());
        assertFalse(narration.hasAudio());
        assertNull(narration.getVoiceName());
    }

    @Test
    void storyWithoutNarrationHasNothing() {
        assertTrue(adapter.find(STORY_ID).isEmpty());
    }

    @Test
    void publishingEightAudiosAndTheVoiceLeavesAllOfThem() {
        adapter.markInProgress(STORY_ID);

        adapter.publish(STORY_ID, "Abuela", eightPages());

        var narration = adapter.find(STORY_ID).orElseThrow();
        assertEquals(NarrationStatus.COMPLETED, narration.getStatus());
        assertEquals("Abuela", narration.getVoiceName());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), narration.getAudioPages());
        assertArrayEquals(new byte[] {3}, adapter.findAudio(STORY_ID, 3).orElseThrow());
    }

    @Test
    void ifPublishingFailsNoAudioNorVoiceRemains() {
        adapter.markInProgress(STORY_ID);
        failOnSave = true;

        assertThrows(IllegalStateException.class, () -> adapter.publish(STORY_ID, "Abuela", eightPages()));

        // Los ficheros ya escritos se retiran; las filas las deshace el rollback de la transaccion (no simulable aqui).
        assertTrue(files.isEmpty());
    }

    @Test
    void coverAudioIsRejectedAndNothingIsWritten() {
        adapter.markInProgress(STORY_ID);
        Map<Integer, byte[]> withCover = eightPages();
        withCover.put(0, new byte[] {0});

        assertThrows(IllegalArgumentException.class, () -> adapter.publish(STORY_ID, "Abuela", withCover));

        assertTrue(files.isEmpty());
        assertEquals(NarrationStatus.IN_PROGRESS, adapter.find(STORY_ID).orElseThrow().getStatus());
        assertTrue(adapter.findAudio(STORY_ID, 0).isEmpty());
    }

    @Test
    void deletingRemovesAllAudiosAndTheVoice() {
        adapter.markInProgress(STORY_ID);
        adapter.publish(STORY_ID, "Abuela", eightPages());

        adapter.delete(STORY_ID);

        assertTrue(adapter.find(STORY_ID).isEmpty());
        assertTrue(files.isEmpty());
        assertTrue(adapter.findAudio(STORY_ID, 2).isEmpty());
    }

    @Test
    void markingErrorLeavesTheStoryWithoutAudioNorVoice() {
        adapter.markInProgress(STORY_ID);
        adapter.publish(STORY_ID, "Abuela", eightPages());

        adapter.markError(STORY_ID);

        var narration = adapter.find(STORY_ID).orElseThrow();
        assertEquals(NarrationStatus.ERROR, narration.getStatus());
        assertNull(narration.getVoiceName());
        assertFalse(narration.hasAudio());
        assertTrue(files.isEmpty());
    }

    @Test
    void storedVoiceIsACopyOfTheNameAndSurvivesWithoutTheUploadedVoice() {
        adapter.markInProgress(STORY_ID);
        adapter.publish(STORY_ID, "Abuela", eightPages());

        // No hay clave ni consulta a la voz subida: el nombre vive en la propia narracion.
        assertEquals("Abuela", adapter.find(STORY_ID).orElseThrow().getVoiceName());
        assertEquals("Abuela", rows.get(STORY_ID).getVoiceName());
    }
}
