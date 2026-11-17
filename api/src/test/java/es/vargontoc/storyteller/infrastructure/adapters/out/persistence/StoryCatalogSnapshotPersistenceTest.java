package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaRepository;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

class StoryCatalogSnapshotPersistenceTest {

    @Test
    void persistsCatalogReferenceAndCopiesApprovedPortraitIntoTheStorySnapshot() {
        var validator = mockValidator();
        var topics = mock(TopicJpaRepository.class);
        var stories = mock(StoryJpaRepository.class);
        var characters = mock(CharacterJpaRepository.class);
        var storage = mock(ResourceStorage.class);
        var catalogCharacters = mock(CatalogCharacterJpaRepository.class);
        var portraits = mock(CatalogCharacterPortraitJpaRepository.class);
        var mapper = mockMapper();
        var topic = new TopicJpaEntity();
        topic.setId(3L);
        var story = new StoryJpaEntity();
        story.setId(41L);
        var catalog = catalogCharacter(9L);
        var portrait = new CatalogCharacterPortraitJpaEntity();
        portrait.setCatalogCharacter(catalog);
        portrait.setAssetPath("characters/catalog/luna.png");
        byte[] portraitBytes = new byte[] { 1, 2, 3 };

        when(topics.findById(3L)).thenReturn(Optional.of(topic));
        when(stories.save(any(StoryJpaEntity.class))).thenReturn(story);
        when(catalogCharacters.findById(9L)).thenReturn(Optional.of(catalog));
        when(portraits.findByCatalogCharacterId(9L)).thenReturn(Optional.of(portrait));
        when(characters.save(any(CharacterJpaEntity.class))).thenAnswer(invocation -> {
            CharacterJpaEntity snapshot = invocation.getArgument(0);
            snapshot.setId(71L);
            return snapshot;
        });
        when(storage.getResource("characters/catalog/luna.png")).thenReturn(portraitBytes);
        when(storage.saveImage(41L, KindImage.ACTOR, 71L, portraitBytes))
                .thenReturn("stories/41/characters/71.png");

        adapter(validator, topics, stories, mapper, storage, characters, catalogCharacters, portraits)
                .create(result(), StorySize.S, 3L, 9L);

        ArgumentCaptor<CharacterJpaEntity> snapshotCaptor = ArgumentCaptor.forClass(CharacterJpaEntity.class);
        verify(characters).save(snapshotCaptor.capture());
        CharacterJpaEntity snapshot = snapshotCaptor.getValue();
        assertSame(catalog, snapshot.getCatalogCharacter());
        assertEquals("Luna", snapshot.getName());
        assertEquals("curiosa", snapshot.getNarrativeDescription());
        assertEquals("zorra naranja", snapshot.getVisualDescription());
        assertEquals("stories/41/characters/71.png", snapshot.getImagePath());
        assertNotEquals(portrait.getAssetPath(), snapshot.getImagePath());
        verify(storage).getResource(portrait.getAssetPath());
        verify(storage).saveImage(41L, KindImage.ACTOR, 71L, portraitBytes);
    }

    private StoryRepositoryAdapter adapter(AbstractValidator<StoryAgentResult> validator, TopicJpaRepository topics,
            StoryJpaRepository stories, AbstractMapper<StoryJpaEntity, es.vargontoc.storyteller.domain.model.Story> mapper,
            ResourceStorage storage, CharacterJpaRepository characters, CatalogCharacterJpaRepository catalogCharacters,
            CatalogCharacterPortraitJpaRepository portraits) {
        return new StoryRepositoryAdapter(validator, topics, stories, mapper,
                mock(StoryPageJpaRepository.class), mock(StoryPageReviewJpaRepository.class),
                mock(StoryReviewJpaRepository.class), storage, characters, mock(CharacterReviewJpaRepository.class),
                catalogCharacters, portraits);
    }

    private CatalogCharacterJpaEntity catalogCharacter(Long id) {
        var catalog = mock(CatalogCharacterJpaEntity.class);
        when(catalog.getId()).thenReturn(id);
        when(catalog.getName()).thenReturn("Luna");
        when(catalog.getNarrativeDescription()).thenReturn("curiosa");
        when(catalog.getVisualDescription()).thenReturn("zorra naranja");
        when(catalog.getMetadata()).thenReturn(new ActorMetadata("orange fox", List.of("orange")));
        return catalog;
    }

    private StoryAgentResult result() {
        return new StoryAgentResult("El paseo", "Luna sale al bosque", List.of(
                new CharacterAgentResult(true, "Nombre del agente", "narrativa", "visual", "english", List.of("x"))));
    }

    @SuppressWarnings("unchecked")
    private AbstractValidator<StoryAgentResult> mockValidator() {
        return mock(AbstractValidator.class);
    }

    @SuppressWarnings("unchecked")
    private AbstractMapper<StoryJpaEntity, es.vargontoc.storyteller.domain.model.Story> mockMapper() {
        return mock(AbstractMapper.class);
    }
}
