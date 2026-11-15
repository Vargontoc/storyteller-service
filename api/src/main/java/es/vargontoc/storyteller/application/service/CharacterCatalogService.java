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
import java.util.List;
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
    private final CatalogCharacterPendingReviewReader reviews; private final Optional<CharacterImageConditionPort> images;
    public CharacterCatalogService(CharacterAgent agent, CatalogCharacterRepository repository, CatalogCharacterAgentResultValidator validator, CatalogCharacterPendingReviewReader reviews, Optional<CharacterImageConditionPort> images) {
        this.agent = agent; this.repository = repository; this.validator=validator; this.reviews=reviews; this.images=images;
    }
    @Override @Transactional
    public CatalogCharacterAgentResult create(CatalogCharacterCreateCommand command) {
        CatalogCharacterAgentResult result = command.automatic()
                ? agent.generateAutomatically() : agent.generateFromHint(command.hint());
        if (!ValidationGuard.guard(validator,result,ValidationOptions.defaults()).isValid())
            throw new AppException("El agente devolvió una ficha de catálogo incompleta", HttpStatus.BAD_REQUEST);
        if (!result.accepted()) return result;
        persistAccepted(result);
        return result;
    }
    @Override
    public CatalogCharacter persistAccepted(CatalogCharacterAgentResult result) {
        return repository.create(new CatalogCharacter(null, result.name(), result.narrativeDescription(),
                result.visualDescription(), result.visualDescriptionEn(), result.visualAttributes()));
    }
    public List<CatalogCharacterView> list(){return repository.findAll().stream().map(this::view).toList();}
    public CatalogCharacterView get(Long id){return view(repository.findById(id));}
    @Transactional public CatalogCharacterView rename(Long id,String name){return view(repository.rename(id,name));}
    private CatalogCharacterView view(CatalogCharacter c){CharacterImageConditionPort imagePort=images.orElseThrow(()->new AppException("La condición de retrato no está disponible",HttpStatus.SERVICE_UNAVAILABLE));return new CatalogCharacterView(c.id(),c.name(),c.narrativeDescription(),c.visualDescription(),!imagePort.hasApprovedPortrait(c.id()),reviews.hasPendingReview(c.id()));}
}
