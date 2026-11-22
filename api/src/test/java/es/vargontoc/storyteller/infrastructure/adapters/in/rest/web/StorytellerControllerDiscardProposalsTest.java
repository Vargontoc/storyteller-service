package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.response.ProposalsDiscarded;

/** TASK-020: el endpoint de descartar propuestas responde 200 síncrono con la página y su estado. */
class StorytellerControllerDiscardProposalsTest {

    @Test
    void discardProposalsAnswersOkWithThePageAndItsStatus() {
        PageAcceptance acceptance = mock(PageAcceptance.class);
        ProposalsDiscarded discarded = new ProposalsDiscarded(7L, 40L, PageStatus.ACCEPTED);
        when(acceptance.discardProposals(7L, 40L)).thenReturn(discarded);
        var controller = new StorytellerController(null, null, null, acceptance, null);

        var response = controller.discardProposals(7L, 40L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(discarded, response.getBody().data());
    }
}
