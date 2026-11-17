package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.application.ports.out.PortraitGenerationTaskRegistry;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitCandidateStore;
import es.vargontoc.storyteller.application.ports.out.persistence.PortraitGenerationJobStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortraitGenerationReconciliationService {
    private static final String RESTART_REASON = "La generación de retratos se interrumpió al reiniciar el servidor.";
    private final PortraitGenerationJobStore jobs; private final PortraitCandidateStore candidates;
    private final PortraitWebsocketNotifier notifier; private final PortraitGenerationTaskRegistry tasks;
    public PortraitGenerationReconciliationService(PortraitGenerationJobStore jobs, PortraitCandidateStore candidates,
            PortraitWebsocketNotifier notifier, PortraitGenerationTaskRegistry tasks) { this.jobs=jobs;this.candidates=candidates;this.notifier=notifier;this.tasks=tasks; }
    @Transactional
    public void reconcile() {
        jobs.findRunning().stream().filter(job -> !tasks.isAlive(job.id())).forEach(job -> {
            jobs.fail(job.id(), RESTART_REASON);
            try { candidates.deleteByGenerationJobId(job.id()); } catch (RuntimeException ignored) { }
            try { notifier.error(job.catalogCharacterId(), RESTART_REASON); } catch (RuntimeException ignored) { }
        });
    }
}
