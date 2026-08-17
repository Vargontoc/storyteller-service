package es.vargontoc.storyteller.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryPageGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.StoryPageUseCase;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryPageReviewCommand;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class StoryPageService implements StoryPageGeneration, StoryPageUseCase {

    @Value("classpath:/prompts/new_cover.st")
    private Resource creatCoverResource;
    @Value("classpath:/prompts/new_page.st")
    private Resource createPageResource;


    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;
    
    private final StoryPageRepository repository;
    private final StoryRepository storyRepository;

    

    public StoryPageService(OllamaPort ollama, 
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL)String model,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER) ChatClient client,
        StoryPageRepository repository,
        StoryRepository storyRepository) {
            this.ollama = ollama;
            this.model = model;
            this.client = client;
            this.repository = repository;
            this.storyRepository = storyRepository;
    }

    @Override
    public StoryPage generate(StoryPageGenerateCommand cmd) {
        // 1. Comprobar que el agente esta en el servidor Ollama
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        // 2. Obtenemos la story
        Story story = storyRepository.getStory(cmd.idStory());
        if(cmd.cover())
            return generateCover(story);

        List<StoryPage> pages = story.getPages().stream().filter(x -> x.getPage() != 0).toList();
        int totalPages = pages.size();
        int currentPage = totalPages + 1;
        int maxPages = story.getSize().getPages();

        if(currentPage > maxPages)
            throw new AppException("Has alcanzado el máximo de páginas", HttpStatus.CONFLICT);
        
        StoryPageAgentResult result = client.prompt()
        .user(u -> u.text(createPageResource)
            .param("synopsis", story.getSummary())
            .param("actors", readActors(story.getCharacters()))
            .param("pages", readPages(pages))
            .param("current", currentPage).param("total", maxPages))
        .call().entity(StoryPageAgentResult.class);


        return repository.create(result, story.getId(), currentPage);
    }

    

    private String readPages(List<StoryPage> pages) {
        if(pages.isEmpty())
            return "EMPTY";
        
        return pages.stream()
            .map(c -> c.getPage() + " - ( " + c.getText() + " / " + c.getScene() + " )")
            .collect(Collectors.joining("; "));
    }

    private StoryPage generateCover(Story story) {
        if(story.getPages().stream().anyMatch(p -> p.getPage() == 0))
            throw new AppException("Ya hay una portada para esta story: " + story.getId(), HttpStatus.BAD_REQUEST);

        StoryPageAgentResult result = client.prompt()
        .user(u -> u.text(creatCoverResource)
            .param("synopsis", story.getSummary())
            .param("actors", readActors(story.getCharacters())))
        .call().entity(StoryPageAgentResult.class);

        StoryPage cover = new StoryPage();
        cover.setPage(0);
        cover.setScene(result.promptScene());

        return repository.create(result, story.getId(), 0);
    }

    private String readActors(List<Actor> characters) {
        return characters.stream()
            .map(c -> c.getName() + " ( " + c.getNarrativeDescription() + " / " + c.getVisualDescription() + " )")
            .collect(Collectors.joining("; "));
    }

    @Override
    public StoryPageReview getReview(Long entityId) {
        return null;
    }

    @Override
    public StoryPageReview review(StoryPageReviewCommand review) {
        return null;
    }

    @Override
    public StoryPage confirmReview(ConfirmReviewRequestDto request) {
        return null;
    }

    @Override
    public StoryPage getPage(Long idPage) {
        return repository.getPage(idPage);
    }
    
}
