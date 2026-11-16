package es.vargontoc.storyteller.infrastructure.persistence;
import es.vargontoc.framework.persistence.jpa.entity.AutoIdJpaEntity;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.infrastructure.converters.ActorMetadataConverter;
import org.hibernate.annotations.ColumnTransformer;
import jakarta.persistence.*;
@Entity @Table(name="catalog_character_review") public class CatalogCharacterReviewJpaEntity extends AutoIdJpaEntity {
 @ManyToOne(optional=false) @JoinColumn(name="catalog_character_id") private CatalogCharacterJpaEntity catalogCharacter;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private RevisionStatus status;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private CharacterReviewTarget target;
 @Column(nullable=false,length=500) private String hint;
 @Column(name="candidate_name",length=100) private String candidateName; @Column(name="candidate_narrative",length=1000) private String candidateNarrative; @Column(name="candidate_visual",length=1000) private String candidateVisual;
 @Convert(converter=ActorMetadataConverter.class) @ColumnTransformer(write="?::jsonb") @Column(name="metadata",columnDefinition="jsonb") private ActorMetadata metadata;
 public CatalogCharacterJpaEntity getCatalogCharacter(){return catalogCharacter;} public RevisionStatus getStatus(){return status;} public CharacterReviewTarget getTarget(){return target;} public String getHint(){return hint;} public String getCandidateName(){return candidateName;} public String getCandidateNarrative(){return candidateNarrative;} public String getCandidateVisual(){return candidateVisual;} public ActorMetadata getMetadata(){return metadata;} public void setCatalogCharacter(CatalogCharacterJpaEntity v){catalogCharacter=v;} public void setStatus(RevisionStatus v){status=v;} public void setTarget(CharacterReviewTarget v){target=v;} public void setHint(String v){hint=v;} public void setCandidateName(String v){candidateName=v;} public void setCandidateNarrative(String v){candidateNarrative=v;} public void setCandidateVisual(String v){candidateVisual=v;} public void setMetadata(ActorMetadata v){metadata=v;}
}
