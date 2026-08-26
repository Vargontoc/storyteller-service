package es.vargontoc.storyteller.infrastructure.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaEntity.CharacterDraft;
import es.vargontoc.storyteller.shared.mappers.AbstractReviewMapper;

@Component
public class StoryReviewMapper extends AbstractReviewMapper<StoryReviewJpaEntity, StoryReview> {

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
        return target;
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
                return c;
            })
            .toList();
    }
}
