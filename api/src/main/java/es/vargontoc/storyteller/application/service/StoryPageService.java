package es.vargontoc.storyteller.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.in.generator.StoryPageGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.StoryPageUseCase;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryPageReviewCommand;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorAction;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.domain.response.StoryCoverAgentResult;
import es.vargontoc.storyteller.domain.response.StoryCoverReviewAgentResult;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.domain.response.StoryPageReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class StoryPageService implements StoryPageGeneration, StoryPageUseCase {

    @Value("classpath:/prompts/new_cover.st")
    private Resource creatCoverResource;
    @Value("classpath:/prompts/review_cover.st")
    private Resource reviewCoverResource;

    @Value("classpath:/prompts/new_page.st")
    private Resource createPageResource;
    @Value("classpath:/prompts/review_page.st")
    private Resource reviewPageResource;



    private final ObjectMapper mapper = new ObjectMapper();

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;

    
    private final StoryPageRepository repository;
    private final StoryPageReviewRepository reviewRepositoy;
    private final StoryRepository storyRepository;

    private final DiscoverCharacterService discoverService;
    

    public StoryPageService(OllamaPort ollama,
        DiscoverCharacterService discover,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL)String model,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER) ChatClient client,
        CharacterRepository characterRepository,
        StoryPageRepository repository,
        StoryPageReviewRepository reviewRepository,
        StoryRepository storyRepository) {
            this.ollama = ollama;
            this.model = model;
            this.client = client;
            this.repository = repository;
            this.storyRepository = storyRepository;
            this.reviewRepositoy = reviewRepository;
            this.discoverService = discover;
    }

    @Override
    public StoryPage generate(StoryPageGenerateCommand cmd) {
        // 1. Comprobar que el agente esta en el servidor Ollama
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        // 2. Obtenemos la story
        Story story = storyRepository.getStory(cmd.idStory());
        if(story.getPages().isEmpty())
            return generateCover(story);

        List<StoryPage> pages = story.getPages().stream().filter(x -> x.getPage() != 0).toList();
        int totalPages = pages.size();
        int currentPage = totalPages + 1;
        int maxPages = story.getSize().getPages();

        if(currentPage > maxPages)
            throw new AppException("Has alcanzado el máximo de páginas", HttpStatus.CONFLICT);
        
        StoryPageAgentResult result = client.prompt()
        .user(u -> u.text(createPageResource)
            .param("synopsis", story.getSynopsis())
            .param("actors", readActors(story.getCharacters()))
            .param("summary", story.getAgentSummary())
            .param("current", currentPage).param("total", maxPages))
        .call().entity(StoryPageAgentResult.class);

        var r = repository.create(result, story.getId(), currentPage);
        discoverService.discoverCharacters(result.composition().actors(), story, result.composition().scene());
        return r;
    }

    
    @Override
    public StoryPageReview review(StoryPageReviewCommand review) {
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        StoryPage current =  getPage(review.pageId());
        Story story = storyRepository.getStory(current.getStoryId());

        reviewRepositoy.changeStatus(review.pageId(), RevisionStatus.DISCARDED);

        if(current.getPage() == 0)
            return reviewCover(review, story, current);

        List<StoryPage> pages = story.getPages().stream().filter(x -> x.getPage() != 0 && x.getPage() < current.getPage()).toList();
        int totalPages = pages.size();
        int currentPage = totalPages + 1;
        int maxPages = story.getSize().getPages();

        StoryPageReviewAgentResult result = client.prompt()
            .user(u -> {
                try {
                    u.text(creatCoverResource)
                        .param("synopsis", story.getSynopsis())
                        .param("actors", readActors(story.getCharacters()))
                        .param("summary", story.getAgentSummary())
                        .param("current", currentPage + 1)
                        .param("total", maxPages)
                        .param("text", current.getText())
                        .param("scene", mapper.writeValueAsString(current.getComposition()))
                        .param("target", review.target().name())
                        .param("hint", review.hint());
                } catch (JsonProcessingException e) {
                    throw new AppException(e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            })
            .call().entity(StoryPageReviewAgentResult.class);

        

        return reviewRepositoy.createReview(result, review.pageId(), review.target(), review.hint());
    }

    @Override
    public StoryPageReview getReview(Long entityId) {
        repository.getPage(entityId);
        return reviewRepositoy.getPendingReview(entityId);
    }

    private StoryPage generateCover(Story story) {
        if(story.getPages().stream().anyMatch(p -> p.getPage() == 0))
            throw new AppException("Ya hay una portada para esta story: " + story.getId(), HttpStatus.BAD_REQUEST);

        StoryCoverAgentResult result = client.prompt()
        .user(u -> u.text(creatCoverResource)
            .param("synopsis", story.getSynopsis())
            .param("actors", readActors(story.getCharacters())))
        .call().entity(StoryCoverAgentResult.class);

        StoryPage cover = new StoryPage();
        cover.setPage(0);
        cover.setComposition(result.scene());

        return repository.create(
            new StoryPageAgentResult(story.getTitle(), result.scene(), "")
            , story.getId(), 0);
    }

    private String readActors(List<Actor> characters) {
        return characters.stream()
            .map(c ->"- " + c.getName() + " ( " + c.getNarrativeDescription() + " / " + c.getVisualDescription() + " )")
            .collect(Collectors.joining(";\n"));
    }


    private StoryPageReview reviewCover(StoryPageReviewCommand cmd, Story story, StoryPage page) {
        
            StoryCoverReviewAgentResult result = client.prompt()
                .user(u -> {
                    try {
                        u.text(reviewCoverResource)
                            .param("synopsis", story.getSynopsis())
                            .param("actors", readActors(story.getCharacters()))
                            .param("scene", mapper.writeValueAsString(page.getComposition()))
                            .param("hint", cmd.hint());
                    } catch (JsonProcessingException e) {
                        throw new AppException(e.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                })
                .call().entity(StoryCoverReviewAgentResult.class);
    
            StoryPageReviewAgentResult r = new StoryPageReviewAgentResult(story.getTitle(), result.scene(), result.hintAccepted(), result.rejectedReason());
    
            return reviewRepositoy.createReview(r, cmd.pageId(), cmd.target(), cmd.hint());
    }


    @Override
    public StoryPage confirmReview(ConfirmReviewRequestDto request) {
        // 1. Obtenemos la pagina
        StoryPage current = getPage(request.entityId());

        // 2. Obtenemos el review
        StoryPageReview review = getReview(request.entityId());
        if(review == null)
            throw new AppException("No hay una review pendiente para la story:" + current.getId(), HttpStatus.NOT_FOUND);

        if(request.status() == RevisionStatus.DISCARDED)
            reviewRepositoy.changeStatus(request.entityId(), request.status());
        else if(request.status() == RevisionStatus.CONFIRMED){
            reviewRepositoy.changeStatus(request.entityId(), request.status());
            StoryPage page = repository.update(current.getId(), review);
            
            discoverService.discoverCharacters(page.getComposition().actors(), storyRepository.getStory(page.getId()), page.getComposition().scene());

            return page;
        }

        
        return current;
    }

    @Override
    public StoryPage getPage(Long idPage) {
        return repository.getPage(idPage);
    }
    
}
