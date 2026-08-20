package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.model.TranslateDescription;

public interface PromptTranslatorPort {

    String translateToEnglish(String text);

    TranslateDescription translateCharacter(String txt);
}
