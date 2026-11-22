package es.vargontoc.storyteller.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.application.ports.in.character.CharacterCatalogUseCase;
import es.vargontoc.storyteller.application.ports.out.external.CharacterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterPendingReviewReader;
import es.vargontoc.storyteller.application.ports.out.external.CharacterImageConditionPort;
import es.vargontoc.storyteller.domain.command.CatalogCharacterCreateCommand;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.domain.response.CatalogCharacterView;
import es.vargontoc.storyteller.domain.enums.CatalogPortraitStatus;
import java.util.List;
import es.vargontoc.storyteller.domain.model.PortraitCandidate;
import es.vargontoc.storyteller.application.ports.in.character.PortraitGenerationUseCase;
import es.vargontoc.storyteller.application.ports.in.character.PortraitSelectionUseCase;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.infrastructure.validations.CatalogCharacterAgentResultValidator;
import es.vargontoc.framework.core.validation.ValidationGuard;
import es.vargontoc.framework.core.validation.ValidationOptions;

@Service
public class CharacterCatalogService implements CharacterCatalogUseCase {
    private final CharacterAgent agent;
    private final CatalogCharacterRepository repository;
    private final CatalogCharacterAgentResultValidator validator;
    private final CatalogCharacterPendingReviewReader reviews; private final Optional<CharacterImageConditionPort> images; private final PortraitLockService lock; private final GenerationGatePort gate;
    public CharacterCatalogService(CharacterAgent agent, CatalogCharacterRepository repository, CatalogCharacterAgentResultValidator validator, CatalogCharacterPendingReviewReader reviews, Optional<CharacterImageConditionPort> images, PortraitLockService lock, GenerationGatePort gate) {
        this.agent = agent; this.repository = repository; this.validator=validator; this.reviews=reviews; this.images=images; this.lock=lock; this.gate=gate;
    }
    public CharacterCatalogService(CharacterAgent agent, CatalogCharacterRepository repository, CatalogCharacterAgentResultValidator validator, CatalogCharacterPendingReviewReader reviews, Optional<CharacterImageConditionPort> images, PortraitLockService lock, GenerationGatePort gate, PortraitGenerationUseCase generation, PortraitSelectionUseCase selection, PortraitCandidateStore candidates, ResourceStorage storage) {
        this(agent, repository, validator, reviews, images, lock, gate, generation, selection, candidates, storage, Optional.empty());
    }
    @Autowired
    public CharacterCatalogService(CharacterAgent agent, CatalogCharacterRepository repository, CatalogCharacterAgentResultValidator validator, CatalogCharacterPendingReviewReader reviews, Optional<CharacterImageConditionPort> images, PortraitLockService lock, GenerationGatePort gate, PortraitGenerationUseCase generation, PortraitSelectionUseCase selection, PortraitCandidateStore candidates, ResourceStorage storage, Optional<es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository> portraits) {
        this(agent, repository, validator, reviews, images, lock, gate); this.generation=generation; this.selection=selection; this.candidates=candidates; this.storage=storage; this.portraits=portraits;
    }
    private PortraitGenerationUseCase generation; private PortraitSelectionUseCase selection; private PortraitCandidateStore candidates; private ResourceStorage storage; private Optional<es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository> portraits=Optional.empty();
    @Override @Transactional
    public CatalogCharacterAgentResult create(CatalogCharacterCreateCommand command) {
        acquireTurn(GenerationType.CHARACTER_AGENT);
        try {
            CatalogCharacterAgentResult result = command.automatic()
                    ? agent.generateAutomatically() : agent.generateFromHint(command.hint());
            if (!ValidationGuard.guard(validator,result,ValidationOptions.defaults()).isValid())
                throw new AppException("El agente devolvió una ficha de catálogo incompleta", HttpStatus.BAD_REQUEST);
            if (!result.accepted()) return result;
            persistAccepted(result);
            return result;
        } finally {
            gate.release();
        }
    }
    /** Pide el turno de generación; si hay otra en curso se rechaza con 409. */
    private void acquireTurn(GenerationType type) {
        if (!gate.tryAcquire(type, null))
            throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);
    }
    @Override
    public CatalogCharacter persistAccepted(CatalogCharacterAgentResult result) {
        return repository.create(new CatalogCharacter(null, result.name(), result.narrativeDescription(),
                result.visualDescription(), result.visualDescriptionEn(), result.visualAttributes()));
    }
    public List<CatalogCharacterView> list(){return repository.findAll().stream().map(this::view).toList();}
    public CatalogCharacterView get(Long id){return view(repository.findById(id));}
    @Transactional public CatalogCharacterView rename(Long id,String name){lock.requireUnlocked(id);return view(repository.rename(id,name));}
    /**
     * Pide el turno PORTRAITS y lo entrega a PortraitGenerationService, que lo libera al terminar el trabajo.
     * Si el servicio no llega a aceptar la solicitud (excepción), el turno se libera aquí.
     */
    @Override public void generatePortraits(Long id){
        repository.findById(id);
        acquireTurn(GenerationType.PORTRAITS);
        try { generation.generate(id); } catch (RuntimeException e) { gate.release(); throw e; }
    }
    @Override public List<PortraitCandidate> candidates(Long id){repository.findById(id); return candidates.findByCatalogCharacterId(id);}
    @Override public void selectPortrait(Long id,Long candidateId){selection.select(id,candidateId);}
    @Override public boolean generatingPortraits(Long id){repository.findById(id);return lock.isLocked(id);}
    @Override public byte[] candidateAsset(Long id,Long candidateId){return candidates.findByCatalogCharacterId(id).stream().filter(c->c.id().equals(candidateId)&&c.catalogCharacterId().equals(id)).findFirst().map(c->storage.getResource(c.assetPath())).orElseThrow(()->new AppException("Candidato de retrato no encontrado",HttpStatus.NOT_FOUND));}
    @Override public byte[] approvedPortraitAsset(Long id){repository.findById(id);return portraits.orElseThrow(()->new AppException("El retrato aprobado no está disponible",HttpStatus.SERVICE_UNAVAILABLE)).findByCatalogCharacterId(id).map(p->storage.getResource(p.getAssetPath())).orElseThrow(()->new AppException("No existe retrato aprobado",HttpStatus.NOT_FOUND));}
    private CatalogCharacterView view(CatalogCharacter c){Boolean draft=images.map(port->!port.hasApprovedPortrait(c.id())).orElse(null);CatalogPortraitStatus status=draft==null?CatalogPortraitStatus.UNKNOWN:(draft?CatalogPortraitStatus.DRAFT:CatalogPortraitStatus.APPROVED);return new CatalogCharacterView(c.id(),c.name(),c.narrativeDescription(),c.visualDescription(),draft,status,reviews.hasPendingReview(c.id()));}
}
