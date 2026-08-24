package es.vargontoc.storyteller.infrastructure.validations;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Component
public class ActorValidation extends AbstractValidator<Actor>  {

    @Override
    public void validate(Actor target) {

        requireNonBlank(target.getNarrativeDescription(), "visual description");
        requireMaxLength(target.getVisualDescription(), 1000, "visual description");

        requireNonBlank(target.getNarrativeDescription(), "role description");
        requireMaxLength(target.getNarrativeDescription(), 1000, "role description");
    }
    
}
