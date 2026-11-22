package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.response.VersionSelected;

/** TASK-017: el endpoint de elegir versión responde 200 síncrono con la versión vigente y su aviso. */
class StorytellerControllerSelectVersionTest {

    @Test
    void selectVersionAnswersOkWithTheCurrentVersionAndItsWarning() {
        PageAcceptance acceptance = mock(PageAcceptance.class);
        VersionSelected selected = new VersionSelected(7L, 40L, 91L, PageLengthWarning.TOO_MANY_WORDS);
        when(acceptance.selectVersion(7L, 40L, 91L)).thenReturn(selected);
        var controller = new StorytellerController(null, null, null, acceptance, null);

        var response = controller.selectVersion(7L, 40L, 91L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(selected, response.getBody().data());
    }
}
