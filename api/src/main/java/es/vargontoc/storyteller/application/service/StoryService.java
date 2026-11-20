package es.vargontoc.storyteller.application.service;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.application.ports.out.external.CharacterImageConditionPort;
import es.vargontoc.storyteller.application.ports.out.external.DirectorAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.TopicRepository;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryReviewCommand;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.domain.request.DirectorReviewRequest;
import es.vargontoc.storyteller.domain.request.DirectorScriptRequest;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.domain.response.DirectorBeat;
import es.vargontoc.storyteller.domain.response.DirectorCastMember;
import es.vargontoc.storyteller.domain.response.DirectorReviewResult;
import es.vargontoc.storyteller.domain.response.DirectorScriptResult;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;

/**
 * Caso de uso del guion: crea el cuento con protagonista de catálogo y escaleta, aprueba el guion
 * y gestiona la revisión global (hint) de la escaleta a través del director.
 */
@Service
@Transactional
public class StoryService implements StoryGeneration {

    private static final int MAX_ATTEMPTS = 3;

    private final StoryRepository repository;
    private final StoryReviewRepository reviewRepository;
    private final StoryOutlineRepository outlineRepository;
    private final TopicRepository topicRepository;
    private final CatalogCharacterRepository catalog;
    private final CharacterImageConditionPort portraits;
    private final PortraitLockService portraitLock;
    private final DirectorAgent director;
    private final Random random = new Random();

    public StoryService(StoryRepository repository,
            TopicRepository topicRepository,
            StoryReviewRepository reviewRepository,
            StoryOutlineRepository outlineRepository,
            DirectorAgent director,
            CatalogCharacterRepository catalog,
            CharacterImageConditionPort portraits,
            PortraitLockService portraitLock) {
        this.repository = repository;
        this.topicRepository = topicRepository;
        this.reviewRepository = reviewRepository;
        this.outlineRepository = outlineRepository;
        this.director = director;
        this.catalog = catalog;
        this.portraits = portraits;
        this.portraitLock = portraitLock;
    }

    @Override
    public Story generate(StoryGenerateCommand cmd) {
        // 1. Protagonista de catálogo con retrato aprobado: el indicado o uno al azar entre los disponibles
        CatalogCharacter protagonist = resolveProtagonist(cmd.catalogCharacterId());

        Topic topic = topicRepository.getTopics().stream()
            .filter(x -> x.getId().equals(cmd.topicId())).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Topic requerido no encontrado"));

        // 2. El director recibe la ficha del protagonista; una escaleta incompleta se reintenta sin persistir nada
        DirectorScriptRequest request = new DirectorScriptRequest(topic.getType(), topic.getDescription(),
            cmd.size().getPages(), protagonist.name(), protagonist.narrativeDescription(),
            protagonist.visualDescription(), protagonist.id());
        DirectorScriptResult script = callDirector(() -> {
            DirectorScriptResult result = director.generateScript(request);
            if (result == null || !toOutline(result.cast(), result.beats()).isComplete()) {
                throw new AppException("Escaleta incompleta", HttpStatus.BAD_REQUEST);
            }
            return result;
        });

        // 3. Persistimos el cuento con la instantánea del protagonista y la escaleta completa
        Story story = repository.create(
            new StoryAgentResult(script.title(), script.synopsis(), List.of(toCharacter(protagonist))),
            cmd.size(), cmd.topicId(), protagonist.id());
        outlineRepository.save(story.getId(), toOutline(script.cast(), script.beats()));
        return repository.getStory(story.getId());
    }

    @Override
    public Story approve(Long storyId) {
        Story story = repository.getStory(storyId);
        requireOutline(story);
        if (story.getStatus() != StoryStatus.SCRIPT_ACCEPTED) {
            repository.updateStatus(storyId, StoryStatus.SCRIPT_ACCEPTED);
        }
        return repository.getStory(storyId);
    }

    @Override
    public StoryReview getReview(Long entityId) {
        repository.getStory(entityId);
        return reviewRepository.getPendingReview(entityId);
    }

    @Override
    public StoryReview review(StoryReviewCommand review) {
        Story current = repository.getStory(review.storyId());
        requireOutline(current);
        StoryOutline outline = outlineRepository.find(review.storyId())
            .orElseThrow(() -> new ResourceNotFoundException("Escaleta no encontrada"));

        // Descartamos la revisión pendiente anterior
        reviewRepository.changeStatus(review.storyId(), RevisionStatus.DISCARDED);

        DirectorReviewRequest request = new DirectorReviewRequest(current.getTitle(), current.getSynopsis(),
            outline.cast().stream().map(c -> new DirectorCastMember(c.name(), c.role())).toList(),
            outline.beats().stream().map(b -> new DirectorBeat(b.event(), b.narrativeFunction(), b.participants(), b.tone(), b.visualNote())).toList(),
            review.hint());
        DirectorReviewResult result = callDirector(() -> {
            DirectorReviewResult r = director.reviewScript(request);
            if (r == null || (!r.hintAccepted() && (r.rejectionReason() == null || r.rejectionReason().isBlank()))
                    || (r.hintAccepted() && !toOutline(r.cast(), r.beats()).isComplete())) {
                throw new AppException("Respuesta de revisión incompleta", HttpStatus.BAD_REQUEST);
            }
            return r;
        });

        // Aceptado: revisión PENDING con la candidata. Rechazado: revisión con el motivo; la escaleta vigente no cambia
        if (!result.hintAccepted()) {
            return reviewRepository.createOutlineReview(review.storyId(), review.hint(), false, result.rejectionReason(), null, null, null);
        }
        return reviewRepository.createOutlineReview(review.storyId(), review.hint(), true, null,
            result.title(), result.synopsis(), toOutline(result.cast(), result.beats()));
    }

    @Override
    public Story confirmReview(ConfirmReviewRequestDto request) {
        Story current = repository.getStory(request.entityId());

        StoryReview review = getReview(request.entityId());
        if (review == null)
            throw new AppException("No hay una review pendiente para la story: " + request.entityId(), HttpStatus.CONFLICT);

        if (request.status() == RevisionStatus.DISCARDED) {
            reviewRepository.changeStatus(request.entityId(), RevisionStatus.DISCARDED);
        } else if (request.status() == RevisionStatus.CONFIRMED) {
            if (review.getCandidateOutline() == null)
                throw new AppException("La revisión pendiente no contiene una escaleta candidata", HttpStatus.CONFLICT);
            // Publica la escaleta candidata como vigente y deja el guion pendiente de aprobación
            reviewRepository.changeStatus(request.entityId(), RevisionStatus.CONFIRMED);
            repository.updateScript(request.entityId(), review.getPreviewStory().getTitle(), review.getPreviewStory().getSynopsis());
            return repository.getStory(request.entityId());
        }
        return current;
    }

    /** Protagonista indicado, o uno al azar entre los de catálogo con retrato aprobado y sin generación de retratos en curso. */
    private CatalogCharacter resolveProtagonist(Long catalogCharacterId) {
        if (catalogCharacterId != null) {
            CatalogCharacter chosen = catalog.findById(catalogCharacterId);
            requireApprovedPortrait(chosen);
            portraitLock.assertCanUseAsStoryProtagonist(catalogCharacterId);
            return chosen;
        }
        List<CatalogCharacter> available = catalog.findAll().stream()
            .filter(c -> portraits.hasApprovedPortrait(c.id()) && !portraitLock.isLocked(c.id()))
            .toList();
        if (available.isEmpty())
            throw new AppException("Debe existir un protagonista del catálogo con retrato aprobado para crear el cuento", HttpStatus.BAD_REQUEST);
        return available.get(random.nextInt(available.size()));
    }

    private void requireApprovedPortrait(CatalogCharacter character) {
        if (!portraits.hasApprovedPortrait(character.id()))
            throw new AppException("El protagonista debe tener un retrato aprobado", HttpStatus.BAD_REQUEST);
    }

    private void requireOutline(Story story) {
        if (!story.hasOutline())
            throw new AppException("El cuento es anterior a la escaleta y no admite aprobación ni revisión del guion", HttpStatus.CONFLICT);
    }

    private <T> T callDirector(Supplier<T> call) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new AppException(
                        "El agente no devolvió una respuesta válida tras " + MAX_ATTEMPTS + " intentos: " + e.getMessage(),
                        HttpStatus.BAD_REQUEST);
                }
            }
        }
        throw new AppException("El agente no devolvió una respuesta válida", HttpStatus.BAD_REQUEST);
    }

    private StoryOutline toOutline(List<DirectorCastMember> cast, List<DirectorBeat> beats) {
        return new StoryOutline(
            cast == null ? List.of() : cast.stream().map(c -> new EpisodicCastMember(c.name(), c.role())).toList(),
            beats == null ? null : beats.stream()
                .map(b -> new StoryBeat(b.event(), b.narrativeFunction(), b.participants(), b.tone(), b.visualNote())).toList());
    }

    private CharacterAgentResult toCharacter(CatalogCharacter c) {
        return new CharacterAgentResult(true, c.name(), c.narrativeDescription(), c.visualDescription(),
            c.visualDescriptionEn(), c.visualAttributes());
    }
}
