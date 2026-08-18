package es.vargontoc.storyteller.application.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.ApiUseCase;
import es.vargontoc.storyteller.application.ports.out.external.ChatterboxPort;
import es.vargontoc.storyteller.application.ports.out.external.ComfyUIPort;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.domain.response.ApiStateResponse;
import es.vargontoc.storyteller.domain.response.OllamaApiResponse;
import es.vargontoc.storyteller.shared.Constants;

@Service
public class ApiService implements ApiUseCase {
    
    private final ChatterboxPort chatterbox;
    private final ComfyUIPort comfui;
    private final OllamaPort ollama;

    private final String topicModel;
    private final String directorModel;
    private final String writerModel;
    public ApiService(ChatterboxPort chatterbox, ComfyUIPort comfui, OllamaPort ollama,
        @Qualifier(Constants.BeanNames.AGENT_TOPICS_MODEL) String topicModel,
        @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String directorModel,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL) String writerModel
    ) {
        this.chatterbox = chatterbox;
        this.comfui = comfui;
        this.ollama = ollama;

        this.topicModel = topicModel;
        this.directorModel = directorModel;
        this.writerModel = writerModel;
    }
    @Override
    public ApiStateResponse getServicesStatus() {
        boolean stateTopic = ollama.isAvailable(topicModel);
        boolean topicTopic = ollama.isAvailable(directorModel);
        boolean writerTopic = ollama.isAvailable(writerModel);


        return new ApiStateResponse(new OllamaApiResponse(stateTopic, topicTopic, writerTopic),
            chatterbox.isAvailableService(),
            comfui.isAvailableService());
    }

    
}
