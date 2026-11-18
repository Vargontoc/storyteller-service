package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.model.CharacterPortraitPrompt;
import java.util.List;

public interface PortraitGeneratorPort {
    List<byte[]> generatePortraits(CharacterPortraitPrompt prompt);
}
