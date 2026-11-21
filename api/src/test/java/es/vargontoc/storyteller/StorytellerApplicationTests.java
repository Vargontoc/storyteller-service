package es.vargontoc.storyteller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"app.agents.character-provisioning.enabled=false", "app.agents.director-provisioning.enabled=false", "app.agents.writer-provisioning.enabled=false","app.portraits.reconciliation.enabled=false"})
class StorytellerApplicationTests {

    @Test
    void contextLoads() { }
}
