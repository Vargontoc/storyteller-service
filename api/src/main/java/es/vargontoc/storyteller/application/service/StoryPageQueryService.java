package es.vargontoc.storyteller.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.application.ports.in.persistence.PageQuery;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.PageAction;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.AcceptedContent;
import es.vargontoc.storyteller.domain.response.PageDetail;
import es.vargontoc.storyteller.domain.response.PageVersionView;
import es.vargontoc.storyteller.domain.response.StoryPagesView;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;

/**
 * Consulta de la portada y las páginas de los cuentos con escaleta. Calcula en el servidor las acciones que
 * el usuario puede hacer en cada página; no escribe nada.
 */
@Service
@Transactional
public class StoryPageQueryService implements PageQuery {

    private final StoryRepository stories;
    private final StoryPageVersionRepository versions;
    private final StoryOutlineRepository outlines;
    private final GenerationGatePort gate;
    private final ResourceStorage storage;

    private final StoryNarrationUseCase narration;

    public StoryPageQueryService(StoryRepository stories, StoryPageVersionRepository versions,
            StoryOutlineRepository outlines, GenerationGatePort gate, ResourceStorage storage,
            StoryNarrationUseCase narration) {
        this.stories = stories;
        this.versions = versions;
        this.outlines = outlines;
        this.gate = gate;
        this.storage = storage;
        this.narration = narration;
    }

    @Override
    public Optional<StoryPagesView> findPages(Long storyId) {
        Story story = stories.getStory(storyId);
        if(!story.hasOutline())
            return Optional.empty();

        boolean generating = gate.isGeneratingStory(storyId);
        List<StoryPage> sorted = story.getPages().stream().sorted(Comparator.comparingInt(StoryPage::getPage)).toList();
        boolean audio = narration.hasAudio(storyId);
        List<PageDetail> details = sorted.stream().map(p -> toDetail(p, generating, audio)).toList();
        return Optional.of(new StoryPagesView(storyId, story.getStatus(), generating, nextPage(story, sorted, generating),
            audio, narration.isNarrating(storyId), details));
    }

    @Override
    public Optional<PageDetail> findPage(Long storyId, Long pageId) {
        Story story = stories.getStory(storyId);
        if(!story.hasOutline())
            return Optional.empty();
        return Optional.of(toDetail(pageOf(story, pageId), gate.isGeneratingStory(storyId), narration.hasAudio(storyId)));
    }

    @Override
    public byte[] getAcceptedImage(Long storyId, Long pageId) {
        StoryPage page = pageOf(stories.getStory(storyId), pageId);
        String image = page.getPageStatus() == PageStatus.ACCEPTED ? page.getImportedImage() : null;
        byte[] content = image == null || image.isBlank() ? null : storage.getResource(image);
        if(content == null)
            throw new ResourceNotFoundException("La página " + pageId + " no tiene imagen aceptada");
        return content;
    }

    private StoryPage pageOf(Story story, Long pageId) {
        return story.getPages().stream().filter(p -> pageId.equals(p.getId())).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Page not found with id: " + pageId + " for story:" + story.getId()));
    }

    /** El audio se publica atómicamente para todas las páginas interiores: con audio en el cuento, toda interior lo tiene. */
    private PageDetail toDetail(StoryPage page, boolean generating, boolean storyHasAudio) {
        boolean pending = page.getPageStatus() == PageStatus.PENDING;
        List<PageVersion> history = versions.findByPage(page.getId());
        List<PageVersionView> views = history.stream()
            .map(v -> new PageVersionView(v.getId(), v.getText(), sceneText(v.getScene()), v.getWarning(), v.isCurrent())).toList();
        AcceptedContent accepted = pending ? null
            : new AcceptedContent(page.getText(), sceneText(page.getComposition()), page.getImportedImage() != null);
        return new PageDetail(page.getId(), page.getPage(), page.getPageStatus(), page.getBeatId(),
            page.getPage() == 0 ? page.getCoverText() : null, accepted,
            pending ? views : List.of(), pending ? List.of() : views,
            generating ? List.of() : actions(pending, history, legacyAcceptable(page)), storyHasAudio && page.getPage() > 0);
    }

    /**
     * Pendiente: aceptar si la vigente no avisa de frases, hint siempre y regenerar solo si avisa de frases.
     * Aceptada: hint (la reabre) y, con propuestas, aceptarla si la vigente no avisa de frases y descartarlas.
     * Elegir versión si hay más de una.
     */
    private List<PageAction> actions(boolean pending, List<PageVersion> history, boolean legacyAcceptable) {
        boolean sentences = history.stream().filter(PageVersion::isCurrent).findFirst()
            .map(v -> v.getWarning().hasSentenceWarning()).orElse(false);
        boolean hasCurrent = history.stream().anyMatch(PageVersion::isCurrent);
        List<PageAction> actions = new ArrayList<>();
        if(pending) {
            if(!hasCurrent)
                return actions;
            if(!sentences)
                actions.add(PageAction.ACCEPT);
            actions.add(PageAction.HINT);
            if(sentences)
                actions.add(PageAction.REGENERATE);
        } else {
            // Página interior con texto y sin estado (anterior al estado por página): se puede aceptar tal cual
            if(legacyAcceptable)
                actions.add(PageAction.ACCEPT);
            actions.add(PageAction.HINT);
        }
        if(history.size() > 1)
            actions.add(PageAction.SELECT_VERSION);
        if(!pending && hasCurrent) {
            if(!sentences)
                actions.add(PageAction.ACCEPT_PROPOSAL);
            actions.add(PageAction.DISCARD_PROPOSALS);
        }
        return actions;
    }

    private static boolean legacyAcceptable(StoryPage page) {
        return page.getPageStatus() == null && page.getPage() > 0 && page.getText() != null && !page.getText().isBlank();
    }

    /** Lo que se puede generar ahora: 0 la portada, N la página N; null si hay algo pendiente, cuento completo o generación en curso. */
    private Integer nextPage(Story story, List<StoryPage> sorted, boolean generating) {
        if(generating || story.getStatus() != StoryStatus.SCRIPT_ACCEPTED)
            return null;
        if(sorted.isEmpty())
            return 0;
        if(sorted.stream().anyMatch(p -> p.getPageStatus() != PageStatus.ACCEPTED))
            return null;
        int beats = outlines.find(story.getId()).map(o -> o.beats().size()).orElse(0);
        // Con la portada y las páginas 1..k aceptadas, la siguiente es la k+1 (= nº de páginas existentes)
        return sorted.size() <= beats ? sorted.size() : null;
    }

    private static String sceneText(SceneComposition scene) {
        return scene == null ? null : scene.scene();
    }
}
