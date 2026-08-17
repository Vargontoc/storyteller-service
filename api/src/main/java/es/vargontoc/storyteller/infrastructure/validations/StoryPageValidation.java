package es.vargontoc.storyteller.infrastructure.validations;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Component
public class StoryPageValidation extends AbstractValidator<StoryPageAgentResult> {

    @Override
    public void validate(StoryPageAgentResult target) {

        requireNonBlank(target.text(), "title");
        requireMaxLength(target.text(), 500, "title");
        requireNonBlank(target.promptScene(), "title");
        requireMaxLength(target.promptScene(), 2000, "title");
    }
    
}
