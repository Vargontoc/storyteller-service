package es.vargontoc.storyteller.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.shared.ReviewBaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_review")
public class StoryReviewJpaEntity extends ReviewBaseEntity {
    
    @ManyToOne(optional = false)
    private StoryJpaEntity story;

    @Column(name = "candidate_title", length = 30)
    private String candidateTitle;

    @Column(name = "candidate_synopsis", length = 500)
    private String candidateSynopsis;

    @ElementCollection
    @CollectionTable(name = "story_review_character_draft", joinColumns = @JoinColumn(name = "review_id"))
    private List<CharacterDraft> candidateCharacters = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RevisionStatus status;

    public StoryReviewJpaEntity() { super(); }

    public StoryJpaEntity getStory() { return story; }
    public void setStory(StoryJpaEntity story) { this.story = story; }
    public String getCandidateTitle() { return candidateTitle; }
    public void setCandidateTitle(String candidateTitle) { this.candidateTitle = candidateTitle; }
    public String getCandidateSynopsis() { return candidateSynopsis; }
    public void setCandidateSynopsis(String candidateSynopsis) { this.candidateSynopsis = candidateSynopsis; }
    public List<CharacterDraft> getCandidateCharacters() { return candidateCharacters; }
    public void setCandidateCharacters(List<CharacterDraft> candidateCharacters) { this.candidateCharacters = candidateCharacters; }

    public static StoryReviewJpaEntity candidate(
        StoryJpaEntity story,
        String hint,
        boolean hintAccepted,
        String rejectedReason,
        String title,
        String synopsis,
        List<CharacterDraft> characters){

            StoryReviewJpaEntity result = new StoryReviewJpaEntity();
            result.setStory(story);
            result.setHint(hint);
            result.setHintAccepted(hintAccepted);
            result.setRejectedReason(rejectedReason);
            result.setCandidateTitle(title);
            result.setCandidateSynopsis(synopsis);
            result.setCandidateCharacters(characters);
            if(hintAccepted)
                result.setStatus(RevisionStatus.PENDING);
            else
                result.setStatus(RevisionStatus.DISCARDED);
            
            result.setCreatedAt(LocalDateTime.now());
            return result;
    }

    @Embeddable
    public static class CharacterDraft {
        
        @Column(name = "name", length = 20)
        private String name;
        @Column(name = "narrative_description", length = 1000)
        private String narrativeDescription;
        @Column(name = "visual_description", length = 1000)
        private String visualDescription;

        protected CharacterDraft() {}

        public CharacterDraft(String name, String narrativeDescription, String visualDescription){
            this.name = name;
            this.narrativeDescription = narrativeDescription;
            this.visualDescription = visualDescription;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getNarrativeDescription() { return narrativeDescription; }
        public void setNarrativeDescription(String narrativeDescription) { this.narrativeDescription = narrativeDescription; }
        public String getVisualDescription() { return visualDescription; }
        public void setVisualDescription(String visualDescription) { this.visualDescription = visualDescription; }
    }
}
