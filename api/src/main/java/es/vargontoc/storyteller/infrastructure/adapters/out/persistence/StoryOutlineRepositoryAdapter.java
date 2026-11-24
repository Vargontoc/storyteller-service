package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryScriptVersion;
import es.vargontoc.storyteller.infrastructure.persistence.EpisodicCastMemberJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryBeatJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryOutlineJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryOutlineJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Repository
public class StoryOutlineRepositoryAdapter implements StoryOutlineRepository {

    private static final String SEPARATOR = "\n";

    private final StoryJpaRepository stories;
    private final StoryOutlineJpaRepository outlines;

    public StoryOutlineRepositoryAdapter(StoryJpaRepository stories, StoryOutlineJpaRepository outlines) {
        this.stories = stories;
        this.outlines = outlines;
    }

    @Override
    @Transactional
    public void save(Long storyId, StoryOutline domain) {
        if (domain == null || !domain.isComplete()) {
            throw new IllegalArgumentException("Incomplete story outline for story " + storyId);
        }
        List<EpisodicCastMember> cast = domain.cast();
        List<StoryBeat> beats = domain.beats();
        StoryJpaEntity story = stories.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));

        // Reemplazo completo: se borra la escaleta previa antes de insertar para no chocar con la unicidad por pagina.
        outlines.findByStoryId(storyId).ifPresent(previous -> {
            outlines.delete(previous);
            outlines.flush();
        });

        LocalDateTime now = LocalDateTime.now();
        StoryOutlineJpaEntity outline = new StoryOutlineJpaEntity();
        outline.setStory(story);
        outline.setCreatedAt(now);
        if (domain.cover() != null) {
            outline.setCoverSetting(domain.cover().setting());
            outline.setCoverTone(domain.cover().tone());
        }
        for (int i = 0; i < beats.size(); i++) {
            outline.getBeats().add(toEntity(outline, i + 1, beats.get(i), now));
        }
        for (int i = 0; i < cast.size(); i++) {
            outline.getCast().add(toEntity(outline, i, cast.get(i), now));
        }
        outlines.save(outline);

        story.setScriptVersion(StoryScriptVersion.OUTLINE);
        stories.save(story);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryOutline> find(Long storyId) {
        return outlines.findByStoryId(storyId).map(outline -> new StoryOutline(
                outline.getCast().stream().map(c -> new EpisodicCastMember(c.getName(), c.getRole())).toList(),
                outline.getBeats().stream().map(this::toBeat).toList(),
                toCover(outline)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findBeatId(Long storyId, int page) {
        return outlines.findByStoryId(storyId).flatMap(outline -> outline.getBeats().stream()
                .filter(b -> b.getPageNumber() == page)
                .map(StoryBeatJpaEntity::getId)
                .findFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> scriptVersion(Long storyId) {
        return stories.findById(storyId).map(StoryJpaEntity::getScriptVersion);
    }

    private StoryBeatJpaEntity toEntity(StoryOutlineJpaEntity outline, int page, StoryBeat beat, LocalDateTime now) {
        StoryBeatJpaEntity entity = new StoryBeatJpaEntity();
        entity.setOutline(outline);
        entity.setPageNumber(page);
        entity.setEvent(beat.event());
        entity.setNarrativeFunction(beat.narrativeFunction());
        entity.setParticipants(String.join(SEPARATOR, beat.participants()));
        entity.setTone(beat.tone());
        entity.setVisualNote(beat.visualNote());
        entity.setSetting(beat.setting());
        entity.setCreatedAt(now);
        return entity;
    }

    private EpisodicCastMemberJpaEntity toEntity(StoryOutlineJpaEntity outline, int position, EpisodicCastMember member,
            LocalDateTime now) {
        EpisodicCastMemberJpaEntity entity = new EpisodicCastMemberJpaEntity();
        entity.setOutline(outline);
        entity.setPosition(position);
        entity.setName(member.name());
        entity.setRole(member.role());
        entity.setCreatedAt(now);
        return entity;
    }

    private StoryBeat toBeat(StoryBeatJpaEntity entity) {
        List<String> participants = entity.getParticipants().isEmpty()
                ? new ArrayList<>()
                : Arrays.asList(entity.getParticipants().split(SEPARATOR));
        return new StoryBeat(entity.getEvent(), entity.getNarrativeFunction(), participants, entity.getTone(),
                entity.getVisualNote(), entity.getSetting());
    }

    /** Sin lugar ni tono guardados (escaleta anterior a la feature) no hay planteamiento de portada. */
    private CoverPlan toCover(StoryOutlineJpaEntity outline) {
        if (outline.getCoverSetting() == null && outline.getCoverTone() == null) {
            return null;
        }
        return new CoverPlan(outline.getCoverSetting(), outline.getCoverTone());
    }
}
