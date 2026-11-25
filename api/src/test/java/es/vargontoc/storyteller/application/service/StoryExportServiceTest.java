package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.ExportMissing;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.ExportFault;
import es.vargontoc.storyteller.domain.response.ExportResult;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

/** TASK-025: exportación para Nubi Game con evaluación completa de faltas. */
class StoryExportServiceTest {

    private static final Long STORY = 7L;
    private static final int PAGES = StorySize.S.getPages();

    private final Map<String, byte[]> files = new HashMap<>();
    private final Map<Integer, String> audioPaths = new HashMap<>();
    private StoryRepository stories;
    private StoryOutlineRepository outlines;
    private StoryExportService service;
    private Story story;

    @BeforeEach
    void setUp() throws IOException {
        stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        NarrationRepository narrations = mock(NarrationRepository.class);
        ResourceStorage storage = mock(ResourceStorage.class);
        when(storage.existsAsset(anyString())).thenAnswer(i -> files.containsKey(i.<String>getArgument(0)));
        when(storage.getResource(anyString())).thenAnswer(i -> files.get(i.<String>getArgument(0)));
        when(narrations.findAudioPath(anyLong(), anyInt()))
            .thenAnswer(i -> Optional.ofNullable(audioPaths.get(i.<Integer>getArgument(1))));
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), List.of(), new CoverPlan("habitación", "calma"))));
        service = new StoryExportService(stories, outlines, narrations, storage,
            new NubiGameExporter(storage, new ImageContentInspector()),
            mock(es.vargontoc.storyteller.application.ports.out.persistence.StoryProtagonistSnapshotReader.class),
            mock(VisualAgentExporter.class));

        story = new Story();
        story.setId(STORY);
        story.setTitle("La luna dormida");
        story.setSize(StorySize.S);
        List<StoryPage> pages = new ArrayList<>();
        pages.add(page(0, PageStatus.ACCEPTED, null, true));
        for (int n = 1; n <= PAGES; n++) {
            pages.add(page(n, PageStatus.ACCEPTED, "Texto " + n, true));
            audioPaths.put(n, "aud" + n);
            files.put("aud" + n, ("wav" + n).getBytes());
        }
        story.setPages(pages);
        when(stories.getStory(STORY)).thenReturn(story);
    }

    private StoryPage page(int number, PageStatus status, String text, boolean withImage) throws IOException {
        StoryPage page = new StoryPage();
        page.setPage(number);
        page.setPageStatus(status);
        page.setText(text);
        if (withImage) {
            page.setImportedImage("img" + number);
            files.put("img" + number, image("png"));
        }
        return page;
    }

    private static byte[] image(String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_3BYTE_BGR), format, out);
        return out.toByteArray();
    }

    private StoryPage pageNumber(int number) {
        return story.getPages().stream().filter(p -> p.getPage() == number).findFirst().orElseThrow();
    }

    private static List<String> summary(ExportResult result) {
        return result.faults().stream().map(f -> f.page() + ":" + f.missing()).toList();
    }

    private static int entries(byte[] zip) throws IOException {
        int count = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                count++;
            }
        }
        return count;
    }

    @Test
    void completeStoryDeliversTheZipWithoutCoverAudio() throws IOException {
        ExportResult result = service.exportForNubiGame(STORY);

        assertTrue(result.isDelivered());
        assertTrue(result.faults().isEmpty());
        // story.json + imagen de portada + imagen y audio por página; ningún audio de portada
        assertEquals(1 + 1 + PAGES * 2, entries(result.zip()));
    }

    @Test
    void missingPageImageAndPageAudioAreReportedTogetherWithoutZip() {
        files.remove("img2");
        files.remove("aud4");

        ExportResult result = service.exportForNubiGame(STORY);

        assertFalse(result.isDelivered());
        assertNull(result.zip());
        assertEquals(List.of("2:IMAGE", "4:AUDIO"), summary(result));
        ExportFault imageFault = result.faults().get(0);
        assertEquals("imagen", imageFault.message());
        assertEquals("audio", result.faults().get(1).message());
    }

    @Test
    void pageWithoutNarrationRecordIsAudioFault() {
        audioPaths.remove(3);

        assertEquals(List.of("3:AUDIO"), summary(service.exportForNubiGame(STORY)));
    }

    @Test
    void missingCoverImageIsReportedOnTheCover() {
        pageNumber(0).setImportedImage(null);

        ExportResult result = service.exportForNubiGame(STORY);

        assertFalse(result.isDelivered());
        assertEquals(List.of("0:IMAGE"), summary(result));
    }

    @Test
    void pendingCoverIsReportedAsUnaccepted() {
        pageNumber(0).setPageStatus(PageStatus.PENDING);

        ExportResult result = service.exportForNubiGame(STORY);

        assertEquals(List.of("0:COVER_NOT_ACCEPTED"), summary(result));
        assertEquals("portada sin aceptar", result.faults().get(0).message());
    }

    @Test
    void legacyStoryWithImagesAndAudioIsRejectedAsCoverUnaccepted() {
        // Anterior a la feature: sin estado por página ni escaleta, aunque tenga imágenes y audio
        story.getPages().forEach(p -> p.setPageStatus(null));
        when(outlines.find(STORY)).thenReturn(Optional.empty());

        ExportResult result = service.exportForNubiGame(STORY);

        assertFalse(result.isDelivered());
        assertEquals(ExportMissing.COVER_NOT_ACCEPTED, result.faults().get(0).missing());
        assertEquals(0, result.faults().get(0).page());
    }

    @Test
    void acceptedCoverWithoutCoverPlanInTheOutlineIsUnaccepted() {
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), List.of(), new CoverPlan("habitación", " "))));

        assertEquals(List.of("0:COVER_NOT_ACCEPTED"), summary(service.exportForNubiGame(STORY)));

        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), List.of())));

        assertEquals(List.of("0:COVER_NOT_ACCEPTED"), summary(service.exportForNubiGame(STORY)));
    }

    @Test
    void unacceptedPageReportsMissingAcceptedTextOnThatPage() {
        pageNumber(3).setPageStatus(PageStatus.PENDING);

        ExportResult result = service.exportForNubiGame(STORY);

        assertEquals(List.of("3:ACCEPTED_TEXT"), summary(result));
        assertEquals("texto aceptado", result.faults().get(0).message());
    }

    @Test
    void notYetGeneratedPagesAreReportedAsUnaccepted() {
        story.getPages().removeIf(p -> p.getPage() > PAGES - 2);

        assertEquals(List.of((PAGES - 1) + ":ACCEPTED_TEXT", PAGES + ":ACCEPTED_TEXT"),
            summary(service.exportForNubiGame(STORY)));
    }

    @Test
    void severalFaultsAreAllReturnedInOnePass() {
        pageNumber(0).setPageStatus(PageStatus.PENDING);
        pageNumber(1).setImportedImage(null);
        pageNumber(2).setPageStatus(null);
        audioPaths.remove(5);

        assertEquals(List.of("0:COVER_NOT_ACCEPTED", "1:IMAGE", "2:ACCEPTED_TEXT", "5:AUDIO"),
            summary(service.exportForNubiGame(STORY)));
    }

    @Test
    void imagesInDifferentFormatsDoNotCauseRejection() throws IOException {
        files.put("img0", image("png"));
        files.put("img1", image("jpg"));
        files.put("img2", image("bmp"));

        ExportResult result = service.exportForNubiGame(STORY);

        assertTrue(result.isDelivered());
        assertNotNull(result.zip());
    }

    @Test
    void unknownStoryPropagatesNotFound() {
        when(stories.getStory(99L)).thenThrow(new ResourceNotFoundException("Story not found with id: 99"));

        assertThrows(ResourceNotFoundException.class, () -> service.exportForNubiGame(99L));
    }
}
