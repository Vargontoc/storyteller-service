package es.vargontoc.storyteller.infrastructure.config;

import java.util.Map;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments; import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier; import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType; import org.springframework.stereotype.Component; import org.springframework.web.client.RestClient;
import org.springframework.ai.ollama.api.OllamaApi;
import es.vargontoc.storyteller.shared.Constants;

@Component @ConditionalOnProperty(name="app.agents.character-provisioning.enabled",havingValue="true",matchIfMissing=true)
public class CharacterModelProvisioner implements ApplicationRunner {
 private static final Logger log=LoggerFactory.getLogger(CharacterModelProvisioner.class);
 private final OllamaApi api; private final RestClient client; private final String name,base,system;
 @Autowired public CharacterModelProvisioner(OllamaApi api,@Value("${spring.ai.ollama.base-url}") String url,@Qualifier(Constants.BeanNames.AGENT_CHARACTER_MODEL) String name,@Value("${app.agents.character-base-model:qwen2.5:14b-instruct}") String base,@Value("${app.agents.character-system:Genera fichas infantiles seguras.}") String system){this(api,RestClient.builder().baseUrl(url).build(),name,base,system);}
 CharacterModelProvisioner(OllamaApi api,RestClient client,String name,String base,String system){this.api=api;this.client=client;this.name=name;this.base=base;this.system=system;}
 @Override public void run(ApplicationArguments args){try{if(api.listModels().models().stream().anyMatch(m->m.name().equals(name)||m.name().startsWith(name+":"))){log.info("Modelo personaje {} disponible",name);return;}client.post().uri("/api/create").contentType(MediaType.APPLICATION_JSON).body(Map.of("model",name,"from",base,"system",system,"stream",false)).retrieve().toBodilessEntity();log.info("Modelo personaje {} creado",name);}catch(Exception e){throw new IllegalStateException("No se pudo aprovisionar el modelo de personaje "+name+" desde "+base,e);}}
}
