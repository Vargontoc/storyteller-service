package es.vargontoc.storyteller.infrastructure.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Component
public class WorkflowTemplateLoader {
    
    private final Resource template;
    private final ComfyUIProperties properties;
    private volatile String cached;

    public WorkflowTemplateLoader(ComfyUIProperties properties, ResourcePatternResolver resolver){
        this.properties = properties;
        this.template = resolver.getResource(properties.workflowCharacterTemplate());
    }

    public String render(String positivePrompt, String negativePrompt, long seed){
        String template = loadTemplate();
        return template
            .replace("{{POSITIVE_PROMPT}}", escapeJson(positivePrompt))
            .replace("{{NEGATIVE_PROMPT}}", escapeJson(negativePrompt))
            .replace("{{SEED}}", String.valueOf(seed))
            .replace("{{LORA_NAME}}", escapeJson(properties.loraName()))
            .replace("{{LORA_STRENGTH_MODEL}}", format(properties.loraStrengthModel()))
            .replace("{{LORA_STRENGTH_CLIP}}", format(properties.loraStrengthClip()));
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String loadTemplate() {
        if(cached == null)
        {
            try{
                cached = new String(template.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }catch(IOException e) {
                throw new ResourceNotFoundException("No se pudo cargar el workflow para ComfyUI");
            }
        }

        return cached;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
