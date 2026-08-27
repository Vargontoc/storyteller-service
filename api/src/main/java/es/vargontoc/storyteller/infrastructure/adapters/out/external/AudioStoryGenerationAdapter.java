package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.in.generator.AudioStoryGeneration;
import es.vargontoc.storyteller.application.ports.out.external.ChatterboxPort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.AudioStoryAgentResult;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import jakarta.transaction.Transactional;

@Component
@Transactional
public class AudioStoryGenerationAdapter implements AudioStoryGeneration  {
    @Value("classpath:/prompts/generate_story_audio.st")
    private Resource userPrompt;

    private final OllamaPort ollama;
    private final ChatterboxPort chatterbox;
    private final String narratorModel;
    private final ChatClient narratorClient;
    private final StoryRepository repository;
    private final PageAudioAssetGenerator pageAudioGenerator;


    public AudioStoryGenerationAdapter(OllamaPort ollama, ChatterboxPort chatterbox,
        @Qualifier(Constants.BeanNames.AGENT_NARRATOR_MODEL) String narratorModel,
        @Qualifier(Constants.BeanNames.AGENT_NARRATOR) ChatClient narratorClient, StoryRepository repository,
        PageAudioAssetGenerator pageAudioGenerator) {

        this.ollama = ollama;
        this.chatterbox = chatterbox;
        this.narratorModel = narratorModel;
        this.narratorClient = narratorClient;
        this.repository = repository;
        this.pageAudioGenerator = pageAudioGenerator;
    }



    @Override
    public AudioStoryAgentResult generateAudio(Long storyId) {
        // 1. Comnprobar servicios
        if(!ollama.isAvailable(narratorModel))
            throw new AppException("El agente narrador no está disponible", HttpStatus.BAD_REQUEST);
        if(!chatterbox.isAvailableService())
            throw new AppException("El servicio Chatterbox no está disponible", HttpStatus.BAD_REQUEST);

        // 2. Obtener story
        Story story = repository.getStory(storyId);

        // 3. Comprobar numero de paginas
        List<StoryPage> pages = story.getPages().stream().filter(x -> x.getPage() != 0).toList();
        if(story.getSize().getPages() != pages.size()){
            throw new AppException("El cuento esta incompleto, hay generar todas las paginas", HttpStatus.BAD_REQUEST);
        }

        String jsonPages = readPages(pages);
        AudioStoryAgentResult result =  narratorClient.prompt().user(u -> u.text(userPrompt)
                .param("synopsis", story.getSynopsis())
                .param("story", jsonPages))
            .call()
            .entity(AudioStoryAgentResult.class);

        pageAudioGenerator.generatePageAudios(storyId, result.pages());

        return result;
    }



    private String readPages(List<StoryPage> pages) {
        Map<Integer, String> send = new HashMap<>();
        pages.forEach(p -> send.put(p.getPage(), p.getText()));

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(send);
        }catch(Exception e){
            throw new AppException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    
}
