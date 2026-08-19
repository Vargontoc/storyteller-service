package es.vargontoc.storyteller.infrastructure.adapters.in.rest.clients;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import es.vargontoc.storyteller.domain.model.ImageRef;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ComfyUIClient {
    
    private final RestClient restClient;
    private final ObjectMapper mapper;

    public ComfyUIClient(ComfyUIProperties config, ObjectMapper mapper) {
        this.restClient = RestClient.builder().baseUrl(config.baseUrl()).build();
        this.mapper = mapper;
    }

    /** Encola el workflow y devuelve el prompt_id asignado */
    public String queuePrompt(String workflowJson) {
        try {
            JsonNode workflow = mapper.readTree(workflowJson);
            Map<String, Object> body = Map.of("prompt", workflow);

            JsonNode response = restClient.post()
                .uri("/prompt")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            if(response == null || !response.has("prompt_id"))
                throw new AppException("ComfyUI no devolvió prompt_id", HttpStatus.BAD_REQUEST);

            return response.get("prompt_id").asString();
        }catch(Exception e){
            throw e;
        }
    }

    public List<ImageRef> pollHistory(String promptId){
        JsonNode history = restClient.get().uri("/history/{id}", promptId).retrieve().body(JsonNode.class);

        if(history == null || !history.has(promptId))
            return null;

        JsonNode entry = history.get(promptId);
        JsonNode outputs = entry.get("outputs");

        List<ImageRef> images = new ArrayList<>();

        if(outputs == null)
            return images;

        Iterator<String> nodeIds = outputs.propertyNames().iterator();
        while(nodeIds.hasNext()){
            JsonNode node = outputs.get(nodeIds.next());
            if(node.has("images")){

                for(JsonNode img: node.get("images")){
                    images.add(new ImageRef(
                        img.get("filename").asString(),
                        img.path("subfolder").asString(""),
                        img.path("type").asString("output"))
                    );
                }
            }
        }
        return images;
    }

    /** Obtiene la imagen generada en byte[] */
    public byte[] viewImage(ImageRef ref) {
        return restClient.get()
            .uri( u -> u.path("view")
                    .queryParam("filename", ref.filename())
                    .queryParam("subfolder", ref.subfolder())
                    .queryParam("type", ref.type())
                .build())
            .retrieve()
            .body(byte[].class);
            
    }

    
    @SuppressWarnings("unchecked")
    public String upload(Path localImage) {
        try {
            byte[] bytes = Files.readAllBytes(localImage);
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("image", new ByteArrayResource(bytes) {
                @Override
                public @Nullable String getFilename() {
                    // TODO Auto-generated method stub
                    return localImage.getFileName().toString();
                }
            });
            form.add("overwrite", "true");

            Map<String, Object> response = restClient.post()
                .uri("/upload/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(Map.class);

            return (String)response.get("name");
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }
}
