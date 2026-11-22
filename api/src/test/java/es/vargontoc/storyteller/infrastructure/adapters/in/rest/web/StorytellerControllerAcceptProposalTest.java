package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.response.ProposalAccepted;

/** TASK-019: el endpoint de aceptar propuesta responde 200 con el contador o con el estado final. */
class StorytellerControllerAcceptProposalTest {

    @Test
    void withoutConfirmationAnswersOkWithTheCounter() {
        PageAcceptance acceptance = mock(PageAcceptance.class);
        ProposalAccepted pending = new ProposalAccepted(7L, 41L, 2, PageStatus.ACCEPTED, true, 2);
        when(acceptance.acceptProposal(7L, 41L, false)).thenReturn(pending);
        var controller = new StorytellerController(null, null, null, acceptance, null);

        var response = controller.acceptProposal(7L, 41L, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pending, response.getBody().data());
    }

    @Test
    void withConfirmationAnswersOkWithTheFinalState() {
        PageAcceptance acceptance = mock(PageAcceptance.class);
        ProposalAccepted done = new ProposalAccepted(7L, 41L, 2, PageStatus.ACCEPTED, false, 2);
        when(acceptance.acceptProposal(7L, 41L, true)).thenReturn(done);
        var controller = new StorytellerController(null, null, null, acceptance, null);

        var response = controller.acceptProposal(7L, 41L, true);

        assertEquals(done, response.getBody().data());
    }
}
