package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.TopicRepository;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class StoryCatalogCharacterTest {
    @Test
    void optionalCatalogCharacterKeepsTheLegacyCommandShape() {
        assertEquals(null, new StoryGenerateCommand(1L, StorySize.S).catalogCharacterId());
        assertEquals(7L, new StoryGenerateCommand(1L, StorySize.S, 7L).catalogCharacterId());
    }

    @Test
    void runningCatalogCharacterIsRejectedBeforeCallingTheAgentOrCreatingAStory() {
        var stories = mock(StoryRepository.class);
        var topics = mock(TopicRepository.class);
        var reviews = mock(StoryReviewRepository.class);
        var ollama = mock(OllamaPort.class);
        var catalog = mock(CatalogCharacterJpaRepository.class);
        var lock = mock(PortraitLockService.class);
        var character = new CatalogCharacterJpaEntity();
        when(catalog.findById(7L)).thenReturn(Optional.of(character));
        org.mockito.Mockito.doThrow(new es.vargontoc.storyteller.shared.exceptions.AppException("bloqueado", org.springframework.http.HttpStatus.CONFLICT))
                .when(lock).assertCanUseAsStoryProtagonist(7L);
        var service = new es.vargontoc.storyteller.application.service.StoryService(stories, topics, reviews, ollama,
                "director", mock(ChatClient.class), catalog, lock);

        assertThrows(es.vargontoc.storyteller.shared.exceptions.AppException.class,
                () -> service.generate(new StoryGenerateCommand(1L, StorySize.S, 7L)));
        verifyNoInteractions(ollama, stories, topics);
    }
}
