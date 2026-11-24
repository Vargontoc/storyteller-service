package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

class StoryProtagonistSnapshotAdapterTest {

    private final StoryJpaRepository stories = mock(StoryJpaRepository.class);
    private final StoryProtagonistSnapshotAdapter adapter = new StoryProtagonistSnapshotAdapter(stories);

    @Test
    void readsNameVisualDescriptionAndPortraitOfTheSnapshot() {
        var story = story(5L);
        story.getCharacters().add(character(false, "Bruno", "perro marron", "stories/5/bruno.png"));
        story.getCharacters().add(character(true, "Luna", "zorra naranja", "stories/5/luna.png"));
        when(stories.findById(5L)).thenReturn(Optional.of(story));

        assertEquals(Optional.of(new StoryProtagonistSnapshot("Luna", "zorra naranja", "stories/5/luna.png")),
                adapter.find(5L));
    }

    @Test
    void editedCatalogCharacterDoesNotChangeTheSnapshot() {
        var story = story(5L);
        var snapshot = character(true, "Luna", "zorra naranja", "stories/5/luna.png");
        var catalog = new CatalogCharacterJpaEntity();
        snapshot.setCatalogCharacter(catalog);
        story.getCharacters().add(snapshot);
        when(stories.findById(5L)).thenReturn(Optional.of(story));

        // El catalogo se edita despues de crear el cuento
        catalog.setName("Luna Nueva");
        catalog.setVisualDescription("zorra gris");

        var read = adapter.find(5L).orElseThrow();
        assertEquals("Luna", read.name());
        assertEquals("zorra naranja", read.visualDescription());
        assertEquals("stories/5/luna.png", read.portraitPath());
    }

    @Test
    void snapshotWithoutPortraitKeepsItNull() {
        var story = story(5L);
        story.getCharacters().add(character(true, "Luna", "zorra naranja", null));
        when(stories.findById(5L)).thenReturn(Optional.of(story));

        assertEquals(null, adapter.find(5L).orElseThrow().portraitPath());
    }

    @Test
    void storyWithoutProtagonistReturnsEmptyAndMissingStoryFails() {
        var story = story(5L);
        story.getCharacters().add(character(false, "Bruno", "perro", null));
        when(stories.findById(5L)).thenReturn(Optional.of(story));
        when(stories.findById(9L)).thenReturn(Optional.empty());

        assertTrue(adapter.find(5L).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> adapter.find(9L));
    }

    private StoryJpaEntity story(Long id) {
        var story = new StoryJpaEntity();
        story.setId(id);
        return story;
    }

    private CharacterJpaEntity character(boolean main, String name, String visual, String imagePath) {
        var c = CharacterJpaEntity.draft(main, name, visual, "narrativa", null);
        c.setImagePath(imagePath);
        return c;
    }
}
