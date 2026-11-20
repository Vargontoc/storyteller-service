package es.vargontoc.storyteller.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.BeatEditing;
import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.BeatEditCommand;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.response.BeatEditResult;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorBeatVerdict;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;

/**
 * Edición directa de un beat: el director solo valida la edad. Si el texto es válido y hay páginas generadas,
 * se avisa y solo con confirmación se eliminan las páginas, se guarda el beat y el guion vuelve a SCRIPT_PENDING.
 * Todo el cambio ocurre en una sola transacción. Los secundarios del reparto nunca pasan por el catálogo.
 */
@Service
@Transactional
public class BeatEditorService implements BeatEditing {

    private final StoryRepository stories;
    private final StoryOutlineRepository outlines;
    private final StoryPageRepository pages;
    private final DirectorAgent director;

    public BeatEditorService(StoryRepository stories, StoryOutlineRepository outlines,
            StoryPageRepository pages, DirectorAgent director) {
        this.stories = stories;
        this.outlines = outlines;
        this.pages = pages;
        this.director = director;
    }

    @Override
    public BeatEditResult edit(BeatEditCommand cmd) {
        Story story = stories.getStory(cmd.storyId());
        if (!story.hasOutline())
            throw new AppException("El cuento es anterior a la escaleta y no admite edición de beats", HttpStatus.CONFLICT);
        StoryOutline outline = outlines.find(cmd.storyId())
            .orElseThrow(() -> new ResourceNotFoundException("Escaleta no encontrada"));

        int index = cmd.page() - 1;
        if (index < 0 || index >= outline.beats().size())
            throw new AppException("La página " + cmd.page() + " no tiene beat en la escaleta", HttpStatus.BAD_REQUEST);
        StoryBeat beat = cmd.beat();
        if (beat == null || !beat.isComplete())
            throw new AppException("El beat debe tener los cinco campos obligatorios", HttpStatus.BAD_REQUEST);

        // Cancelación del aviso: sin efectos y sin consultar al director
        if (Boolean.FALSE.equals(cmd.confirmInvalidation()))
            return BeatEditResult.cancelled();

        // El director solo da el veredicto de edad; ante rechazo no se muta nada
        DirectorBeatVerdict verdict = director.validateBeat(new DirectorBeat(
            beat.event(), beat.narrativeFunction(), beat.participants(), beat.tone(), beat.visualNote()));
        if (verdict == null || !verdict.accepted())
            return BeatEditResult.rejected(verdict == null ? "El director no devolvió veredicto" : verdict.reason());

        // Antes de aplicar: si hay páginas generadas, aviso con contador y confirmación
        int generated = story.getPages() == null ? 0 : story.getPages().size();
        if (generated > 0 && !Boolean.TRUE.equals(cmd.confirmInvalidation()))
            return BeatEditResult.confirmationRequired(generated);

        int removed = generated > 0 ? pages.deleteByStory(cmd.storyId()) : 0;
        List<StoryBeat> beats = new ArrayList<>(outline.beats());
        beats.set(index, beat);
        outlines.save(cmd.storyId(), new StoryOutline(outline.cast(), beats));
        stories.updateStatus(cmd.storyId(), StoryStatus.SCRIPT_PENDING);
        Story updated = stories.getStory(cmd.storyId());
        if (removed > 0)
            updated.setPages(List.of()); // el borrado es masivo: evita devolver páginas obsoletas de la caché
        return BeatEditResult.applied(updated, removed);
    }
}
