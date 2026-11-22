package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.PageGeneration;
import es.vargontoc.storyteller.domain.response.GenerationStarted;

/** TASK-016: el endpoint de regeneración sin hint responde 202 con la generación iniciada. */
class StorytellerControllerRegenerateTest {

    @Test
    void regeneratePageAnswersAcceptedWithTheStartedGeneration() {
        PageGeneration generation = mock(PageGeneration.class);
        when(generation.startRegenerate(7L, 40L)).thenReturn(new GenerationStarted(7L, 3));
        var controller = new StorytellerController(null, null, generation, null, null);

        var response = controller.regeneratePage(7L, 40L);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(new GenerationStarted(7L, 3), response.getBody().data());
    }
}
