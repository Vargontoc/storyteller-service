package es.vargontoc.storyteller.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;

/**
 * Al arrancar, cierra como fallidas las narraciones que quedaron en curso: las deja en error sin audio ni voz
 * y notifica el fallo. No toca las terminadas. La puerta de generacion vive en memoria y arranca libre.
 */
@Service
public class NarrationReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(NarrationReconciliationService.class);

    private final NarrationRepository narrations;
    private final NarrationNotifier notifier;

    public NarrationReconciliationService(NarrationRepository narrations, NarrationNotifier notifier) {
        this.narrations = narrations;
        this.notifier = notifier;
    }

    public void reconcile() {
        narrations.findInProgress().forEach(narration -> {
            Long storyId = narration.getStoryId();
            try {
                narrations.markError(storyId);
            } catch (RuntimeException e) {
                log.warn("No se pudo reconciliar la narracion del cuento {}: {}", storyId, e.toString());
                return;
            }
            try {
                notifier.failed(storyId);
            } catch (RuntimeException e) {
                log.warn("No se pudo notificar el error de la narracion del cuento {}: {}", storyId, e.toString());
            }
        });
    }
}
