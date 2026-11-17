package es.vargontoc.storyteller.infrastructure.config;

import es.vargontoc.storyteller.application.service.PortraitGenerationReconciliationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.portraits.reconciliation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PortraitGenerationReconciliationRunner implements ApplicationRunner {
    private final PortraitGenerationReconciliationService service;
    public PortraitGenerationReconciliationRunner(PortraitGenerationReconciliationService service) { this.service = service; }
    public void run(ApplicationArguments args) { service.reconcile(); }
}
