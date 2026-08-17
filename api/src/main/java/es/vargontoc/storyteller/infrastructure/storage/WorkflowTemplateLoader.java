package es.vargontoc.storyteller.infrastructure.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.WorkflowProperties;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Component
public class WorkflowTemplateLoader {
    
    private final ResourcePatternResolver resolver;
    private volatile Map<String, String> cached = new HashMap<>();

    public WorkflowTemplateLoader(ComfyUIProperties properties, ResourcePatternResolver resolver){
        this.resolver = resolver;
    }

    public String render(WorkflowProperties properties){
        String template = loadTemplate(properties.path());
        return template
            .replace("{{POSITIVE_PROMPT}}", escapeJson(properties.positive()))
            .replace("{{NEGATIVE_PROMPT}}", escapeJson(properties.negative()))
            .replace("{{SEED}}", String.valueOf(properties.seed()))
            
            .replace("{{WIDTH}}", String.valueOf(properties.dimensions()[0]))
            .replace("{{HEIGHT}}", String.valueOf(properties.dimensions()[1]))
            
            .replace("{{LORA_NAME}}", escapeJson(properties.lora()))
            .replace("{{LORA_STRENGTH_MODEL}}", format(properties.strength()))
            .replace("{{LORA_STRENGTH_CLIP}}", format(properties.clip()));
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String loadTemplate(String path) {
        if(!cached.containsKey(path))
        {
            try{
                var template =  resolver.getResource(path);
                cached.put(path, new String(template.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            }catch(IOException e) {
                throw new ResourceNotFoundException("No se pudo cargar el workflow para ComfyUI");
            }
        }

        return cached.get(path);
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
