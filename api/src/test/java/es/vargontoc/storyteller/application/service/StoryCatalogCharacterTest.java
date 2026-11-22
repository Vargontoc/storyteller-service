package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.out.external.CharacterImageConditionPort;
import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.shared.exceptions.AppException;

class StoryCatalogCharacterTest {
    @Test
    void optionalCatalogCharacterKeepsTheLegacyCommandShape() {
        assertEquals(null, new StoryGenerateCommand("Amistad", "Un paseo", StorySize.S).catalogCharacterId());
        assertEquals(7L, new StoryGenerateCommand("Amistad", "Un paseo", StorySize.S, 7L).catalogCharacterId());
    }

    @Test
    void runningCatalogCharacterIsRejectedBeforeCallingTheDirectorOrCreatingAStory() {
        var stories = mock(StoryRepository.class);
        var director = mock(DirectorAgent.class);
        var catalog = mock(CatalogCharacterRepository.class);
        var images = mock(CharacterImageConditionPort.class);
        var lock = mock(PortraitLockService.class);
        when(catalog.findById(7L)).thenReturn(new CatalogCharacter(7L, "Luna", "n", "v", "e", List.of()));
        when(images.hasApprovedPortrait(7L)).thenReturn(true);
        doThrow(new AppException("bloqueado", HttpStatus.CONFLICT)).when(lock).assertCanUseAsStoryProtagonist(7L);
        var service = new StoryService(stories, mock(StoryReviewRepository.class),
                mock(StoryOutlineRepository.class), director, catalog, images, lock, new GenerationGate());

        assertThrows(AppException.class, () -> service.generate(new StoryGenerateCommand("Amistad", "Un paseo", StorySize.S, 7L)));
        verifyNoInteractions(director, stories);
    }
}
