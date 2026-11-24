package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.GlobalExceptionHandler;

/** TASK-014: la subida multipart entrega los bytes al caso de uso y traduce el rechazo. */
class StorytellerControllerUploadImageTest {

    private PageAcceptance acceptance;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        acceptance = mock(PageAcceptance.class);
        mvc = MockMvcBuilders.standaloneSetup(new StorytellerController(null, null, null, acceptance, null))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void uploadAnswersOkAndPassesTheOriginalBytes() throws Exception {
        StoryPage page = new StoryPage();
        page.setId(40L);
        page.setPage(1);
        page.setPageStatus(PageStatus.ACCEPTED);
        byte[] content = {1, 2, 3};
        when(acceptance.uploadImage(7L, 40L, content)).thenReturn(page);

        mvc.perform(multipart("/api/v1/storyteller/7/pages/40/image").file(new MockMultipartFile("file", "x.png", "image/png", content)))
            .andExpect(status().isOk());

        verify(acceptance).uploadImage(7L, 40L, content);
    }

    @Test
    void rejectionIsAnsweredWithItsStatus() throws Exception {
        when(acceptance.uploadImage(any(), any(), any()))
            .thenThrow(new AppException("El fichero no es una imagen", HttpStatus.UNSUPPORTED_MEDIA_TYPE));

        mvc.perform(multipart("/api/v1/storyteller/7/pages/40/image").file(new MockMultipartFile("file", "x.txt", "text/plain", new byte[] {9})))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingFileIsPassedAsEmptyContent() throws Exception {
        when(acceptance.uploadImage(any(), any(), any()))
            .thenThrow(new AppException("El fichero no es una imagen", HttpStatus.UNSUPPORTED_MEDIA_TYPE));

        mvc.perform(multipart("/api/v1/storyteller/7/pages/40/image"))
            .andExpect(status().isUnsupportedMediaType());

        verify(acceptance).uploadImage(7L, 40L, new byte[0]);
    }
}
