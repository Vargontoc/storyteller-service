package es.vargontoc.storyteller.infrastructure.validations;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Component
public class StoryPageValidation extends AbstractValidator<StoryPageAgentResult> {

    @Override
    public void validate(StoryPageAgentResult target) {

        requireNonBlank(target.text(), "text");
        requireMaxLength(target.text(), 500, "text");
        requireNonBlank(target.promptScene(), "scene");
        requireMaxLength(target.promptScene(), 2000, "scene");
    }
    
}
