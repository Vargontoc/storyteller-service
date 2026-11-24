package es.vargontoc.storyteller.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WriterModelProvisionerContextTest {

    @Test
    void productionConstructorIsInjectable() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.ai.ollama.base-url=http://localhost:11434",
                        "app.agents.writer-provisioning.enabled=true")
                .withBean("scriptwriter-model", String.class, () -> "storyteller-writer")
                .withBean(WriterModelProvisioner.class)
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertNotNull(context.getBean(WriterModelProvisioner.class));
                });
    }
}
