package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.in.generator.BeatEditing;
import es.vargontoc.storyteller.domain.enums.BeatEditStatus;
import es.vargontoc.storyteller.domain.response.BeatEditResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.BeatEditRequestDto;

class GenerationsControllerBeatTest {

    @Test
    void editBeatPassesStoryPageBeatAndConfirmationToUseCase() {
        BeatEditing editing = mock(BeatEditing.class);
        when(editing.edit(org.mockito.ArgumentMatchers.any())).thenReturn(BeatEditResult.confirmationRequired(3));
        var controller = new GenerationsController(null, null, null, editing, null);

        var response = controller.editBeat(7L, 2,
            new BeatEditRequestDto("Luna sale", "nudo", List.of("Luna"), "curioso", "bosque", "claro del bosque", true));

        assertTrue(response.getBody().success());
        assertEquals(BeatEditStatus.CONFIRMATION_REQUIRED, response.getBody().data().status());
        assertEquals(3, response.getBody().data().invalidatedPages());
        verify(editing).edit(argThat(c -> c.storyId().equals(7L) && c.page() == 2
            && c.beat().event().equals("Luna sale") && Boolean.TRUE.equals(c.confirmInvalidation())));
    }

    @Test
    void getScriptWrapsUseCaseResultInApiResponse() {
        var reading = mock(es.vargontoc.storyteller.application.ports.in.generator.StoryScriptReading.class);
        var script = new es.vargontoc.storyteller.domain.response.StoryScript(
            7L, "T", "S", es.vargontoc.storyteller.domain.enums.StoryStatus.SCRIPT_PENDING, null, List.of(), List.of());
        when(reading.getScript(7L)).thenReturn(script);
        var controller = new GenerationsController(null, null, null, null, reading);

        var response = controller.getScript(7L);

        assertTrue(response.getBody().success());
        assertEquals(script, response.getBody().data());
    }
}
