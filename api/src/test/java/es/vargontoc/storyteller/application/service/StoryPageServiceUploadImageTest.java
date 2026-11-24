package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.shared.exceptions.AppException;

/** TASK-014: subida de imagen en la portada y en las páginas aceptadas. Repositorio y almacén en memoria. */
class StoryPageServiceUploadImageTest {

    private static final Long STORY = 5L;

    private Map<Long, StoryPage> stored;
    private Map<String, byte[]> files;
    private StoryPageRepository pages;
    private ResourceStorage storage;
    private StoryPageService service;
    private int fileCounter;

    @BeforeEach
    void setUp() {
        stored = new HashMap<>();
        files = new HashMap<>();
        pages = mock(StoryPageRepository.class);
        storage = mock(ResourceStorage.class);
        when(pages.getPage(anyLong())).thenAnswer(i -> stored.get(i.getArgument(0)));
        when(pages.setImportedImage(anyLong(), anyString())).thenAnswer(i -> {
            StoryPage p = stored.get(i.<Long>getArgument(0));
            p.setImportedImage(i.getArgument(1));
            return p;
        });
        // El almacén guarda la nueva y retira la anterior de la misma página, como el adaptador real
        when(storage.saveImportedImage(anyLong(), anyLong(), any())).thenAnswer(i -> {
            String prefix = "p" + i.getArgument(1) + "/";
            files.keySet().removeIf(k -> k.startsWith(prefix));
            String path = prefix + (++fileCounter);
            files.put(path, i.getArgument(2));
            return path;
        });
        when(storage.getResource(anyString())).thenAnswer(i -> files.get(i.<String>getArgument(0)));
        service = new StoryPageService(mock(OllamaPort.class), "writer", mock(ChatClient.class), pages,
            mock(StoryPageReviewRepository.class), mock(StoryRepository.class), mock(StoryOutlineRepository.class),
            mock(GenerationGatePort.class), mock(WriterAgent.class), mock(StoryPageVersionRepository.class),
            mock(StoryPageNotifier.class), Runnable::run, mock(StoryNarrationUseCase.class), storage, new ImageContentInspector());
    }

    private StoryPage page(long id, int number, PageStatus status) {
        StoryPage page = new StoryPage();
        page.setId(id);
        page.setStoryId(STORY);
        page.setPage(number);
        page.setPageStatus(status);
        stored.put(id, page);
        return page;
    }

    @Test
    void pngIsAcceptedAndAssociatedToTheAcceptedPage() throws IOException {
        StoryPage page = page(40L, 1, PageStatus.ACCEPTED);
        byte[] png = encode(300, 200, "png");

        StoryPage result = service.uploadImage(STORY, 40L, png);

        assertEquals(page.getImportedImage(), result.getImportedImage());
        assertArrayEquals(png, storage.getResource(result.getImportedImage()));
    }

    @Test
    void jpegOver20MbIsAcceptedOnTheAcceptedCoverAsIs() throws IOException {
        StoryPage cover = page(30L, 0, PageStatus.ACCEPTED);
        byte[] jpeg = encode(64, 64, "jpg");
        byte[] big = new byte[21 * 1024 * 1024];
        System.arraycopy(jpeg, 0, big, 0, jpeg.length);

        service.uploadImage(STORY, 30L, big);

        // Se guarda el original, sin convertir ni recortar
        assertArrayEquals(big, storage.getResource(cover.getImportedImage()));
    }

    @Test
    void onlyThePageThatReceivesTheUploadHasAnImage() throws IOException {
        StoryPage one = page(41L, 1, PageStatus.ACCEPTED);
        StoryPage two = page(42L, 2, PageStatus.ACCEPTED);
        StoryPage three = page(43L, 3, PageStatus.ACCEPTED);

        service.uploadImage(STORY, 43L, encode(10, 10, "png"));

        assertNull(one.getImportedImage());
        assertNull(two.getImportedImage());
        assertEquals(true, three.getImportedImage() != null);
    }

    @Test
    void secondUploadReplacesTheFirstOne() throws IOException {
        StoryPage page = page(41L, 1, PageStatus.ACCEPTED);
        byte[] a = encode(10, 10, "png");
        byte[] b = encode(20, 20, "bmp");
        service.uploadImage(STORY, 41L, a);
        String pathA = page.getImportedImage();

        service.uploadImage(STORY, 41L, b);

        assertArrayEquals(b, storage.getResource(page.getImportedImage()));
        assertNull(storage.getResource(pathA));
    }

    @Test
    void textFileIsRejectedAndThePreviousImageIsKept() throws IOException {
        StoryPage page = page(41L, 1, PageStatus.ACCEPTED);
        byte[] a = encode(10, 10, "png");
        service.uploadImage(STORY, 41L, a);
        String pathA = page.getImportedImage();

        AppException e = assertThrows(AppException.class,
            () -> service.uploadImage(STORY, 41L, "hola".getBytes(StandardCharsets.UTF_8)));

        assertEquals("El fichero no es una imagen", e.getMessage());
        assertEquals(pathA, page.getImportedImage());
        assertArrayEquals(a, storage.getResource(pathA));
        verify(storage).saveImportedImage(anyLong(), anyLong(), any());
    }

    @Test
    void coverNotAcceptedIsRejectedWithoutImage() throws IOException {
        StoryPage cover = page(30L, 0, PageStatus.PENDING);

        AppException e = assertThrows(AppException.class, () -> service.uploadImage(STORY, 30L, encode(10, 10, "png")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertNull(cover.getImportedImage());
        verify(storage, never()).saveImportedImage(anyLong(), anyLong(), any());
    }

    @Test
    void pendingPageIsRejectedWithoutImage() throws IOException {
        StoryPage page = page(41L, 1, PageStatus.PENDING);

        assertThrows(AppException.class, () -> service.uploadImage(STORY, 41L, encode(10, 10, "png")));

        assertNull(page.getImportedImage());
        verify(storage, never()).saveImportedImage(anyLong(), anyLong(), any());
    }

    @Test
    void pageOfAnotherStoryIsNotFound() throws IOException {
        page(41L, 1, PageStatus.ACCEPTED);

        AppException e = assertThrows(AppException.class, () -> service.uploadImage(99L, 41L, encode(10, 10, "png")));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    private byte[] encode(int w, int h, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }
}
