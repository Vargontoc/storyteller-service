package es.vargontoc.storyteller.infrastructure.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity.CandidateBeat;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity.CandidateCastMember;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity.CharacterDraft;
import es.vargontoc.storyteller.shared.mappers.AbstractReviewMapper;

@Component
public class StoryReviewMapper extends AbstractReviewMapper<StoryReviewJpaEntity, StoryReview> {

    private static final String SEPARATOR = "\n";

    @Override
    public StoryReviewJpaEntity toEntity(StoryReview model) {
        StoryReviewJpaEntity target = new StoryReviewJpaEntity();
        target.setId(model.getId());
        target.setHint(model.getHint());
        target.setHintAccepted(model.isHintAccepted());
        target.setRejectedReason(model.getRejectedReason());
        target.setStatus(model.getStatus());
        target.setCandidateTitle(model.getPreviewStory().getTitle());
        target.setCandidateSynopsis(model.getPreviewStory().getSynopsis());
        target.setCandidateCharacters(toDrafts(model.getPreviewStory().getCharacters()));
        if (model.getCandidateOutline() != null) {
            fillOutline(target, model.getCandidateOutline(), model.getChangedBeats());
        }

        StoryJpaEntity story = new StoryJpaEntity();
        story.setId(model.getPreviewStory().getId());
        target.setStory(story);

        return target;
    }

    @Override
    public StoryReview toModel(StoryReviewJpaEntity entity) {
        StoryReview target = new StoryReview();
        target.setId(entity.getId());
        target.setHint(entity.getHint());
        target.setHintAccepted(entity.isHintAccepted());
        target.setRejectedReason(entity.getRejectedReason());
        target.setPreviewStory(readStory(entity));

        target.setStatus(entity.getStatus());
        if (!entity.getCandidateBeats().isEmpty()) {
            target.setCandidateOutline(toOutline(entity));
            target.setChangedBeats(new ArrayList<>(entity.getChangedBeats()));
        }
        return target;
    }

    /** Copia la escaleta candidata y sus beats distintos a la entidad de revision. */
    public void fillOutline(StoryReviewJpaEntity target, StoryOutline outline, List<Integer> changedBeats) {
        target.setCandidateBeats(outline.beats().stream()
            .map(b -> new CandidateBeat(b.event(), b.narrativeFunction(), String.join(SEPARATOR, b.participants()), b.tone(), b.visualNote(), b.setting()))
            .toList());
        target.setCandidateCoverSetting(outline.cover() == null ? null : outline.cover().setting());
        target.setCandidateCoverTone(outline.cover() == null ? null : outline.cover().tone());
        target.setCandidateCast(outline.cast().stream().map(c -> new CandidateCastMember(c.name(), c.role())).toList());
        target.setChangedBeats(changedBeats == null ? new ArrayList<>() : new ArrayList<>(changedBeats));
    }

    /** Escaleta candidata de la revision, lista para publicarla como vigente. */
    public StoryOutline toOutline(StoryReviewJpaEntity entity) {
        List<StoryBeat> beats = entity.getCandidateBeats().stream()
            .map(b -> new StoryBeat(b.getEvent(), b.getNarrativeFunction(), Arrays.asList(b.getParticipants().split(SEPARATOR)), b.getTone(), b.getVisualNote(), b.getSetting()))
            .toList();
        List<EpisodicCastMember> cast = entity.getCandidateCast().stream()
            .map(c -> new EpisodicCastMember(c.getName(), c.getRole()))
            .toList();
        boolean noCover = entity.getCandidateCoverSetting() == null && entity.getCandidateCoverTone() == null;
        CoverPlan cover = noCover ? null : new CoverPlan(entity.getCandidateCoverSetting(), entity.getCandidateCoverTone());
        return new StoryOutline(cast, beats, cover);
    }

    private Story readStory(StoryReviewJpaEntity entity){
        
        Story target = new Story();
        target.setTitle(entity.getCandidateTitle());
        target.setSynopsis(entity.getCandidateSynopsis());
        target.setCharacters(toCharacters(entity.getCandidateCharacters()));
        return target;
    }

    private List<CharacterDraft> toDrafts(List<Actor> characters) {
        if (characters == null) {
            return new ArrayList<>();
        }
        return characters.stream()
            .map(c -> new CharacterDraft(c.isMain(), c.getName(), c.getNarrativeDescription(), c.getVisualDescription(), c.getMetadata()))
            .toList();
    }

    private List<Actor> toCharacters(List<CharacterDraft> drafts) {
        if (drafts == null) {
            return new ArrayList<>();
        }
        return drafts.stream()
            .map(d -> {
                Actor c = new Actor();
                c.setName(d.getName());
                c.setNarrativeDescription(d.getNarrativeDescription());
                c.setVisualDescription(d.getVisualDescription());
                c.setMetadata(d.getMetadata());
                return c;
            })
            .toList();
    }
}
