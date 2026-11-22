package es.vargontoc.storyteller.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.service.NarrationReconciliationService;

/** Dispara la reconciliacion de narraciones al arrancar (mismo patron que la de retratos). */
@Component
@ConditionalOnProperty(prefix = "app.narration.reconciliation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NarrationReconciliationRunner implements ApplicationRunner {
    private final NarrationReconciliationService service;

    public NarrationReconciliationRunner(NarrationReconciliationService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.reconcile();
    }
}
