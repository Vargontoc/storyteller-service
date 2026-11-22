package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.CoverGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.application.ports.in.generator.PageGeneration;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.response.PageAccepted;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.PageHintRequestDto;

class StorytellerControllerCoverTest {

    @Test
    void generateCoverAnswersAcceptedWithTheStartedGeneration() {
        CoverGeneration generation = mock(CoverGeneration.class);
        when(generation.startCover(7L)).thenReturn(new GenerationStarted(7L, 0));
        var controller = new StorytellerController(null, generation, null, null, null);

        var response = controller.generateCover(7L);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals(new GenerationStarted(7L, 0), response.getBody().data());
    }

    @Test
    void generatePageAnswersAcceptedWithTheStartedGeneration() {
        PageGeneration generation = mock(PageGeneration.class);
        when(generation.startPage(7L, 3)).thenReturn(new GenerationStarted(7L, 3));
        var controller = new StorytellerController(null, null, generation, null, null);

        var response = controller.generatePage(7L, 3);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(new GenerationStarted(7L, 3), response.getBody().data());
    }

    @Test
    void acceptPageAnswersOkWithTheAcceptedPage() {
        PageAcceptance acceptance = mock(PageAcceptance.class);
        StoryPage page = new StoryPage();
        page.setId(40L);
        page.setPage(0);
        page.setPageStatus(PageStatus.ACCEPTED);
        when(acceptance.acceptPage(7L, 40L)).thenReturn(page);
        var controller = new StorytellerController(null, null, null, acceptance, null);

        var response = controller.acceptPage(7L, 40L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PageAccepted(7L, 40L, 0, PageStatus.ACCEPTED), response.getBody().data());
    }

    @Test
    void hintPageAnswersAcceptedWithTheStartedGeneration() {
        PageGeneration generation = mock(PageGeneration.class);
        when(generation.startHint(7L, 40L, "que el conejo esté contento")).thenReturn(new GenerationStarted(7L, 3));
        var controller = new StorytellerController(null, null, generation, null, null);

        var response = controller.hintPage(7L, 40L, new PageHintRequestDto("que el conejo esté contento"));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(new GenerationStarted(7L, 3), response.getBody().data());
    }
}
