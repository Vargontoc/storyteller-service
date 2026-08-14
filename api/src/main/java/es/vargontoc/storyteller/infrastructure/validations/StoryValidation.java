package es.vargontoc.storyteller.infrastructure.validations;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.CharacterAgentResult;
import es.vargontoc.storyteller.domain.StoryAgentResult;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Component
public class StoryValidation extends AbstractValidator<StoryAgentResult> {

    @Override
    public void validate(StoryAgentResult target) {

        requireNonBlank(target.title(), "title");
        requireMaxLength(target.title(), 50, "title");
        requireNonBlank(target.synopsis(), "synopsis");
        requireMaxLength(target.synopsis(), 500, "synopsis");
        requiredItems(target.characters(),  "characters");

        for(int i = 0; i < target.characters().size(); i++)
            validateCharacter(target.characters().get(i), i);

    }

    private void validateCharacter(CharacterAgentResult target, int i) {
        requireNonBlank(target.name(), String.format("characters[%d].name",  i));
        requireMaxLength(target.name(), 20, String.format("characters[%d].name", i));
        
        requireNonBlank(target.narrativeDescription(), String.format("characters[%d].narrativeDescription",  i));
        requireMaxLength(target.narrativeDescription(), 1000, String.format("characters[%d].narrativeDescription", i));
        
        requireNonBlank(target.visualDescription(), String.format("characters[%d].visualDescription",  i));
        requireMaxLength(target.visualDescription(), 1000, String.format("characters[%d].visualDescription", i));

    }
    
}
