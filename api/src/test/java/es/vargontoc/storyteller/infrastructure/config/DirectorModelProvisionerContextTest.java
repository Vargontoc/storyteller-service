package es.vargontoc.storyteller.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DirectorModelProvisionerContextTest {

    @Test
    void productionConstructorIsInjectable() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.ai.ollama.base-url=http://localhost:11434",
                        "app.agents.director-provisioning.enabled=true")
                .withBean(OllamaApi.class, () -> org.mockito.Mockito.mock(OllamaApi.class))
                .withBean("director-model", String.class, () -> "storyteller-director")
                .withBean(DirectorModelProvisioner.class)
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertNotNull(context.getBean(DirectorModelProvisioner.class));
                });
    }
}
