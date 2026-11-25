package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.vargontoc.storyteller.application.ports.in.export.StoryExport;
import es.vargontoc.storyteller.application.ports.in.generator.CoverGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.application.ports.in.generator.PageGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.PageQuery;
import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.domain.enums.ExportMissing;
import es.vargontoc.storyteller.domain.response.ExportFault;
import es.vargontoc.storyteller.domain.response.ExportResult;

/** TASK-025: ruta REST de la exportación para Nubi Game y baja de la ruta heredada. */
class StoryExportControllerTest {

    private StoryExport export;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        export = mock(StoryExport.class);
        StorytellerController legacy = new StorytellerController(mock(StorytellerUseCase.class), mock(CoverGeneration.class),
            mock(PageGeneration.class), mock(PageAcceptance.class), mock(PageQuery.class));
        mvc = MockMvcBuilders.standaloneSetup(legacy, new StoryExportController(export)).build();
    }

    @Test
    void deliveredExportRespondsWithTheZip() throws Exception {
        byte[] zip = { 1, 2, 3 };
        when(export.exportForNubiGame(7L)).thenReturn(ExportResult.delivered(zip));

        byte[] body = mvc.perform(get("/api/v1/storyteller/7/export/nubi-game"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(zip, body);
    }

    @Test
    void rejectedExportRespondsWithAllFaultsAndNoZip() throws Exception {
        when(export.exportForNubiGame(7L)).thenReturn(ExportResult.rejected(List.of(
            new ExportFault(2, ExportMissing.IMAGE, "imagen"),
            new ExportFault(4, ExportMissing.AUDIO, "audio"))));

        mvc.perform(get("/api/v1/storyteller/7/export/nubi-game"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data.faults.length()").value(2))
            .andExpect(jsonPath("$.data.faults[0].page").value(2))
            .andExpect(jsonPath("$.data.faults[0].missing").value("IMAGE"))
            .andExpect(jsonPath("$.data.faults[1].page").value(4))
            .andExpect(jsonPath("$.data.faults[1].missing").value("AUDIO"));
    }

    @Test
    void visualAgentDeliveredRespondsWithTheZip() throws Exception {
        byte[] zip = { 4, 5 };
        when(export.exportForVisualAgent(7L)).thenReturn(ExportResult.delivered(zip));

        byte[] body = mvc.perform(get("/api/v1/storyteller/7/export/visual-agent"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(zip, body);
    }

    @Test
    void visualAgentRejectedRespondsWithAllFaultsAndCodes() throws Exception {
        when(export.exportForVisualAgent(7L)).thenReturn(ExportResult.rejected(List.of(
            new ExportFault(3, ExportMissing.ACCEPTED_TEXT, "texto aceptado"),
            new ExportFault(ExportFault.STORY, ExportMissing.CONTRACT_RULE, "demasiado pequeña", "CHARACTER_IMAGE_TOO_SMALL"))));

        mvc.perform(get("/api/v1/storyteller/7/export/visual-agent"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.data.faults.length()").value(2))
            .andExpect(jsonPath("$.data.faults[0].page").value(3))
            .andExpect(jsonPath("$.data.faults[1].page").value(-1))
            .andExpect(jsonPath("$.data.faults[1].missing").value("CONTRACT_RULE"))
            .andExpect(jsonPath("$.data.faults[1].code").value("CHARACTER_IMAGE_TOO_SMALL"));
    }

    @Test
    void previousDownloadRouteNoLongerRespondsWithAZip() throws Exception {
        int status = mvc.perform(get("/api/v1/storyteller/7/download")).andReturn().getResponse().getStatus();

        assertEquals(404, status);
    }
}
