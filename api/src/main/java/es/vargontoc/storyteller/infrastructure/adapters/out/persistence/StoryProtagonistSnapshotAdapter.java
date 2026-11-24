package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryProtagonistSnapshotReader;
import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

/** Lee la instantánea del protagonista del propio cuento (personaje principal), sin pasar por el catálogo. */
@Repository
public class StoryProtagonistSnapshotAdapter implements StoryProtagonistSnapshotReader {

    private final StoryJpaRepository stories;

    public StoryProtagonistSnapshotAdapter(StoryJpaRepository stories) {
        this.stories = stories;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryProtagonistSnapshot> find(Long storyId) {
        var story = stories.findById(storyId)
            .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));
        return story.getCharacters().stream()
            .filter(c -> c.isMain())
            .findFirst()
            .map(c -> new StoryProtagonistSnapshot(c.getName(), c.getVisualDescription(), c.getImagePath()));
    }
}
