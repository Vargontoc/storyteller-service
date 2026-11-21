package es.vargontoc.storyteller.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import es.vargontoc.storyteller.shared.Constants;

/**
 * Registra en Ollama el modelo del escritor al arrancar, a partir del Modelfile
 * mantenido como recurso del backend. Si el modelo ya existe no hace nada.
 */
@Component
@ConditionalOnProperty(name = "app.agents.writer-provisioning.enabled", havingValue = "true", matchIfMissing = true)
public class WriterModelProvisioner implements ApplicationRunner {

    static final String MODELFILE_PATH = "prompts/writer.modelfile";

    private static final Logger log = LoggerFactory.getLogger(WriterModelProvisioner.class);

    private final OllamaApi api;
    private final RestClient client;
    private final String name;
    private final Resource modelfile;

    @Autowired
    public WriterModelProvisioner(
            OllamaApi api,
            @Value("${spring.ai.ollama.base-url}") String url,
            @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL) String name) {
        this(api, RestClient.builder().baseUrl(url).build(), name, new ClassPathResource(MODELFILE_PATH));
    }

    WriterModelProvisioner(OllamaApi api, RestClient client, String name, Resource modelfile) {
        this.api = api;
        this.client = client;
        this.name = name;
        this.modelfile = modelfile;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (api.listModels().models().stream()
                    .anyMatch(m -> m.name().equals(name) || m.name().startsWith(name + ":"))) {
                log.info("Modelo escritor {} disponible", name);
                return;
            }
            Map<String, Object> request = toRequest(modelfile.getContentAsString(StandardCharsets.UTF_8));
            client.post().uri("/api/create").contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().toBodilessEntity();
            log.info("Modelo escritor {} creado", name);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("No se pudo aprovisionar el modelo del escritor " + name + " desde "
                    + MODELFILE_PATH + ": Ollama respondio " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo aprovisionar el modelo del escritor " + name + " desde "
                    + MODELFILE_PATH + ": " + e, e);
        }
    }

    /** Traduce el Modelfile (FROM, PARAMETER, SYSTEM) al cuerpo de /api/create vigente de Ollama. */
    private Map<String, Object> toRequest(String definition) {
        String from = null;
        String system = null;
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (String raw : definition.lines().toList()) {
            String line = raw.trim();
            if (line.startsWith("FROM ")) {
                from = line.substring(5).trim();
            } else if (line.startsWith("PARAMETER ")) {
                String[] kv = line.substring(10).trim().split("\\s+", 2);
                if (kv.length == 2) {
                    parameters.put(kv[0], number(kv[1]));
                }
            } else if (line.startsWith("SYSTEM ")) {
                String rest = definition.substring(definition.indexOf("SYSTEM ") + 7).trim();
                if (rest.startsWith("\"\"\"")) {
                    int end = rest.indexOf("\"\"\"", 3);
                    system = rest.substring(3, end < 0 ? rest.length() : end).trim();
                } else {
                    system = rest.lines().findFirst().orElse("");
                }
                break;
            }
        }
        if (from == null) {
            throw new IllegalStateException("El Modelfile no define FROM");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", name);
        body.put("from", from);
        if (system != null) {
            body.put("system", system);
        }
        if (!parameters.isEmpty()) {
            body.put("parameters", parameters);
        }
        body.put("stream", false);
        return body;
    }

    private static Object number(String v) {
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            try {
                return Double.valueOf(v);
            } catch (NumberFormatException e2) {
                return v;
            }
        }
    }
}
