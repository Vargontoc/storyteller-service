package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.CharacterBackgroundRemover;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryProtagonistSnapshotReader;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;
import es.vargontoc.storyteller.domain.response.ExportResult;

/** TASK-030: exportación para el agente visual con evaluación completa de condiciones. */
class StoryVisualAgentExportTest {

    private static final Long STORY = 7L;
    private static final int PAGES = StorySize.S.getPages();

    private StoryOutlineRepository outlines;
    private StoryProtagonistSnapshotReader snapshots;
    private CharacterBackgroundRemover remover;
    private StoryExportService service;
    private Story story;
    private List<StoryBeat> beats;

    @BeforeEach
    void setUp() throws IOException {
        StoryRepository stories = mock(StoryRepository.class);
        outlines = mock(StoryOutlineRepository.class);
        snapshots = mock(StoryProtagonistSnapshotReader.class);
        remover = mock(CharacterBackgroundRemover.class);
        ResourceStorage storage = mock(ResourceStorage.class);
        when(storage.getResource("portrait")).thenReturn(new byte[] {1});
        when(snapshots.find(STORY)).thenReturn(Optional.of(
            new StoryProtagonistSnapshot("Nina", "vestido amarillo, pelo rizado", "portrait")));
        when(remover.removeBackground(any())).thenReturn(Optional.of(png(1024)));

        beats = new ArrayList<>();
        for (int n = 1; n <= PAGES; n++) {
            beats.add(new StoryBeat("e" + n, "f", List.of("Nina"), "tono " + n, "n", "lugar " + n));
        }
        when(outlines.find(STORY)).thenReturn(Optional.of(
            new StoryOutline(List.of(), beats, new CoverPlan("habitación al anochecer", "calma"))));

        story = new Story();
        story.setId(STORY);
        story.setTitle("La luna dormida");
        story.setSize(StorySize.S);
        story.setStatus(StoryStatus.PAGES_ACCEPTED);
        List<StoryPage> pages = new ArrayList<>();
        for (int n = 0; n <= PAGES; n++) {
            pages.add(page(n));
        }
        story.setPages(pages);
        when(stories.getStory(STORY)).thenReturn(story);

        service = new StoryExportService(stories, outlines, mock(NarrationRepository.class), storage,
            mock(NubiGameExporter.class), snapshots,
            new VisualAgentExporter(storage, remover, new VisualContractValidator()));
    }

    private static StoryPage page(int number) {
        StoryPage page = new StoryPage();
        page.setPage(number);
        page.setPageStatus(PageStatus.ACCEPTED);
        page.setText(number == 0 ? null : "Texto " + number);
        page.setComposition(new SceneComposition("Escena " + number, List.of(), null));
        return page; // sin imagen ni audio: no se exigen
    }

    private static byte[] png(int side) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    private StoryPage pageNumber(int number) {
        return story.getPages().stream().filter(p -> p.getPage() == number).findFirst().orElseThrow();
    }

    private static List<String> summary(ExportResult result) {
        return result.faults().stream().map(f -> f.page() + ":" + f.missing()).toList();
    }

    @Test
    void newStoryWithoutImagesNorAudioDeliversTheZip() {
        ExportResult result = service.exportForVisualAgent(STORY);

        assertTrue(result.isDelivered());
        assertTrue(result.faults().isEmpty());
    }

    @Test
    void beatsWithoutSettingAreReportedPerPageWithoutZip() {
        beats.set(0, new StoryBeat("e", "f", List.of("Nina"), "t", "n", null));
        beats.set(2, new StoryBeat("e", "f", List.of("Nina"), "t", "n", "  "));

        ExportResult result = service.exportForVisualAgent(STORY);

        assertFalse(result.isDelivered());
        assertNull(result.zip());
        assertEquals(List.of("1:BEAT_SETTING", "3:BEAT_SETTING"), summary(result));
    }

    @Test
    void coverWithoutSettingNorToneIsReported() {
        when(outlines.find(STORY)).thenReturn(Optional.of(new StoryOutline(List.of(), beats, new CoverPlan(" ", null))));

        assertEquals(List.of("0:COVER_SETTING_TONE"), summary(service.exportForVisualAgent(STORY)));
    }

    @Test
    void unacceptedPageIsReported() {
        pageNumber(3).setPageStatus(PageStatus.PENDING);

        assertEquals(List.of("3:ACCEPTED_TEXT"), summary(service.exportForVisualAgent(STORY)));
    }

    @Test
    void unacceptedCoverIsReported() {
        pageNumber(0).setPageStatus(PageStatus.PENDING);

        ExportResult result = service.exportForVisualAgent(STORY);

        assertEquals(List.of("0:COVER_NOT_ACCEPTED"), summary(result));
        assertEquals("portada sin aceptar", result.faults().get(0).message());
    }

    @Test
    void unapprovedScriptIsReported() {
        story.setStatus(StoryStatus.SCRIPT_PENDING);

        assertEquals(List.of("-1:SCRIPT_NOT_APPROVED"), summary(service.exportForVisualAgent(STORY)));
    }

    @Test
    void legacyStoryReportsWhichConditionsAreMissing() {
        story.setScriptVersion(null);
        story.getPages().forEach(p -> p.setPageStatus(null));
        when(outlines.find(STORY)).thenReturn(Optional.empty());

        List<String> faults = summary(service.exportForVisualAgent(STORY));

        assertTrue(faults.contains("0:COVER_NOT_ACCEPTED"));
        assertTrue(faults.contains("0:COVER_SETTING_TONE"));
        assertTrue(faults.contains("1:BEAT_SETTING"));
        assertTrue(faults.contains("1:ACCEPTED_TEXT"));
    }

    @Test
    void severalConditionsAtOnceAreAllReported() {
        story.setStatus(StoryStatus.SCRIPT_PENDING);
        pageNumber(0).setPageStatus(PageStatus.PENDING);
        pageNumber(2).setPageStatus(null);
        beats.set(4, new StoryBeat("e", "f", List.of("Nina"), "t", "n", null));
        when(snapshots.find(STORY)).thenReturn(Optional.empty());

        assertEquals(List.of("-1:SCRIPT_NOT_APPROVED", "0:COVER_NOT_ACCEPTED", "2:ACCEPTED_TEXT", "5:BEAT_SETTING",
            "-1:PROTAGONIST_MISSING"), summary(service.exportForVisualAgent(STORY)));
        verify(remover, never()).removeBackground(any());
    }

    @Test
    void smallPortraitIsRejectedByTheContract() throws IOException {
        when(remover.removeBackground(any())).thenReturn(Optional.of(png(768)));

        ExportResult result = service.exportForVisualAgent(STORY);

        assertFalse(result.isDelivered());
        assertEquals(List.of("-1:CONTRACT_RULE"), summary(result));
        assertEquals("CHARACTER_IMAGE_TOO_SMALL", result.faults().get(0).code());
    }

    @Test
    void backgroundRemovalFailureIsReportedAsProtagonistImage() {
        when(remover.removeBackground(any())).thenReturn(Optional.empty());

        ExportResult result = service.exportForVisualAgent(STORY);

        assertFalse(result.isDelivered());
        assertEquals(List.of("-1:PROTAGONIST_IMAGE"), summary(result));
        assertEquals("CHARACTER_IMAGE_NOT_OBTAINED", result.faults().get(0).code());
    }

    @Test
    void contractRulesAreAllReturnedTogether() {
        story.setTitle("Yo");
        pageNumber(0).setComposition(new SceneComposition(" ", List.of(), null));

        ExportResult result = service.exportForVisualAgent(STORY);

        assertFalse(result.isDelivered());
        List<String> codes = result.faults().stream().map(f -> f.code()).toList();
        assertTrue(codes.contains("STORY_ID_INVALID"));
        assertTrue(codes.contains("SCENE_DESCRIPTION_MISSING"));
    }
}
