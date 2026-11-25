package es.vargontoc.storyteller.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.export.StoryExport;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryProtagonistSnapshotReader;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.ExportMissing;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.NubiGameStory;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;
import es.vargontoc.storyteller.domain.model.VisualAgentStory;
import es.vargontoc.storyteller.domain.response.ExportFault;
import es.vargontoc.storyteller.domain.response.ExportResult;
import jakarta.transaction.Transactional;

/**
 * Orquesta las exportaciones: evalúa todas las condiciones, acumula todas las faltas y entrega el zip o el rechazo.
 * Para Nubi Game solo rechaza por disponibilidad (portada, texto aceptado, imagen y audio): el formato de las
 * imágenes no es motivo de rechazo.
 */
@Service
@Transactional
public class StoryExportService implements StoryExport {

    private final StoryRepository stories;
    private final StoryOutlineRepository outlines;
    private final NarrationRepository narrations;
    private final ResourceStorage storage;
    private final NubiGameExporter nubiGameExporter;
    private final StoryProtagonistSnapshotReader snapshots;
    private final VisualAgentExporter visualAgentExporter;

    public StoryExportService(StoryRepository stories, StoryOutlineRepository outlines, NarrationRepository narrations,
            ResourceStorage storage, NubiGameExporter nubiGameExporter, StoryProtagonistSnapshotReader snapshots,
            VisualAgentExporter visualAgentExporter) {
        this.stories = stories;
        this.outlines = outlines;
        this.narrations = narrations;
        this.storage = storage;
        this.nubiGameExporter = nubiGameExporter;
        this.snapshots = snapshots;
        this.visualAgentExporter = visualAgentExporter;
    }

    @Override
    public ExportResult exportForNubiGame(Long storyId) {
        Story story = stories.getStory(storyId);
        List<StoryPage> pages = story.getPages() == null ? List.of() : story.getPages();
        List<ExportFault> faults = new ArrayList<>();

        // Portada: aceptada y con planteamiento de portada en el guion (los cuentos anteriores no lo tienen)
        StoryPage cover = find(pages, 0);
        if (!isAccepted(cover) || !hasCoverPlan(storyId)) {
            faults.add(new ExportFault(0, ExportMissing.COVER_NOT_ACCEPTED, "portada sin aceptar"));
        }
        // La imagen de la portada solo se exige cuando está aceptada: si no, ya consta como sin aceptar
        if (isAccepted(cover) && !exists(cover.getImportedImage())) {
            faults.add(new ExportFault(0, ExportMissing.IMAGE, "imagen"));
        }

        List<NubiGameStory.Page> exportPages = new ArrayList<>();
        for (int n = 1; n <= story.getSize().getPages(); n++) {
            StoryPage page = find(pages, n);
            if (!isAccepted(page) || page.getText() == null || page.getText().isBlank()) {
                faults.add(new ExportFault(n, ExportMissing.ACCEPTED_TEXT, "texto aceptado"));
                continue;
            }
            String image = page.getImportedImage();
            if (!exists(image)) {
                faults.add(new ExportFault(n, ExportMissing.IMAGE, "imagen"));
            }
            Optional<String> audio = narrations.findAudioPath(storyId, n).filter(this::exists);
            if (audio.isEmpty()) {
                faults.add(new ExportFault(n, ExportMissing.AUDIO, "audio"));
            }
            exportPages.add(new NubiGameStory.Page(n, page.getText(), image, audio.orElse(null)));
        }

        if (!faults.isEmpty()) {
            return ExportResult.rejected(faults);
        }
        return ExportResult.delivered(nubiGameExporter.export(
            new NubiGameStory(story.getTitle(), cover.getImportedImage(), exportPages)));
    }

    /**
     * Agente visual: no exige imágenes ni audio. Acumula las condiciones de guion, portada, páginas y protagonista;
     * solo si se cumplen todas compone el cuento y deja que {@link VisualAgentExporter} aplique el contrato y
     * obtenga la imagen del protagonista. Disponibilidad y contrato se devuelven con la misma forma de rechazo.
     */
    @Override
    public ExportResult exportForVisualAgent(Long storyId) {
        Story story = stories.getStory(storyId);
        List<StoryPage> pages = story.getPages() == null ? List.of() : story.getPages();
        StoryOutline outline = outlines.find(storyId).orElse(null);
        List<StoryBeat> beats = outline == null || outline.beats() == null ? List.of() : outline.beats();
        CoverPlan plan = outline == null ? null : outline.cover();
        List<ExportFault> faults = new ArrayList<>();

        if (story.getStatus() != StoryStatus.SCRIPT_ACCEPTED && story.getStatus() != StoryStatus.PAGES_ACCEPTED) {
            faults.add(new ExportFault(ExportFault.STORY, ExportMissing.SCRIPT_NOT_APPROVED, "guion sin aprobar"));
        }
        StoryPage cover = find(pages, 0);
        if (!isAccepted(cover)) {
            faults.add(new ExportFault(0, ExportMissing.COVER_NOT_ACCEPTED, "portada sin aceptar"));
        }
        if (!isComplete(plan)) {
            faults.add(new ExportFault(0, ExportMissing.COVER_SETTING_TONE, "falta el lugar y el tono de la portada"));
        }

        List<VisualAgentStory.Page> exportPages = new ArrayList<>();
        for (int n = 1; n <= story.getSize().getPages(); n++) {
            StoryPage page = find(pages, n);
            StoryBeat beat = n <= beats.size() ? beats.get(n - 1) : null;
            boolean accepted = isAccepted(page) && notBlank(page.getText());
            if (!accepted) {
                faults.add(new ExportFault(n, ExportMissing.ACCEPTED_TEXT, "texto aceptado"));
            }
            if (beat == null || !notBlank(beat.setting())) {
                faults.add(new ExportFault(n, ExportMissing.BEAT_SETTING, "lugar de escena"));
            }
            if (accepted && beat != null) {
                exportPages.add(new VisualAgentStory.Page(page.getText(), sceneOf(page), beat.setting(), beat.tone()));
            }
        }

        Optional<StoryProtagonistSnapshot> hero = snapshots.find(storyId);
        if (hero.isEmpty()) {
            faults.add(new ExportFault(ExportFault.STORY, ExportMissing.PROTAGONIST_MISSING, "protagonista"));
        }
        if (!faults.isEmpty()) {
            return ExportResult.rejected(faults);
        }

        VisualAgentExporter.Result result = visualAgentExporter.export(new VisualAgentStory(story.getTitle(), hero.get(),
            new VisualAgentStory.Cover(sceneOf(cover), plan.setting(), plan.tone()), exportPages));
        if (result.isValid()) {
            return ExportResult.delivered(result.zip());
        }
        return ExportResult.rejected(result.errors().stream().map(StoryExportService::contractFault).toList());
    }

    private static ExportFault contractFault(VisualContractValidator.Error error) {
        boolean image = error.code().equals("CHARACTER_IMAGE_MISSING") || error.code().equals("CHARACTER_IMAGE_NOT_OBTAINED");
        return new ExportFault(ExportFault.STORY, image ? ExportMissing.PROTAGONIST_IMAGE : ExportMissing.CONTRACT_RULE,
            error.message() + " (" + error.path() + ")", error.code());
    }

    private static String sceneOf(StoryPage page) {
        return page.getComposition() == null ? null : page.getComposition().scene();
    }

    private boolean hasCoverPlan(Long storyId) {
        return isComplete(outlines.find(storyId).map(o -> o.cover()).orElse(null));
    }

    private static boolean isComplete(CoverPlan plan) {
        return plan != null && notBlank(plan.setting()) && notBlank(plan.tone());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static StoryPage find(List<StoryPage> pages, int number) {
        return pages.stream().filter(p -> p.getPage() == number).findFirst().orElse(null);
    }

    private static boolean isAccepted(StoryPage page) {
        return page != null && page.getPageStatus() == PageStatus.ACCEPTED;
    }

    private boolean exists(String path) {
        return storage.existsAsset(path);
    }
}
