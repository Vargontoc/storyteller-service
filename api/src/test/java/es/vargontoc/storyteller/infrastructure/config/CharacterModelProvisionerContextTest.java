package es.vargontoc.storyteller.infrastructure.config;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
class CharacterModelProvisionerContextTest {
 @Test void productionConstructorIsInjectable(){new ApplicationContextRunner().withPropertyValues("spring.ai.ollama.base-url=http://localhost:11434","app.agents.character-base-model=base","app.agents.character-system=system","app.agents.character-provisioning.enabled=true").withBean(OllamaApi.class,()->org.mockito.Mockito.mock(OllamaApi.class)).withBean("character-model",String.class,()->"storyteller-character").withBean(CharacterModelProvisioner.class).run(context->{assertTrue(context.isRunning());assertNotNull(context.getBean(CharacterModelProvisioner.class));});}
}
