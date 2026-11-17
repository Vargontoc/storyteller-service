package es.vargontoc.storyteller.infrastructure.config;

import es.vargontoc.storyteller.application.ports.out.PortraitGenerationTaskRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryPortraitGenerationTaskRegistry implements PortraitGenerationTaskRegistry {
    private final Set<Long> running = ConcurrentHashMap.newKeySet();
    public void register(Long generationJobId) { running.add(generationJobId); }
    public void unregister(Long generationJobId) { running.remove(generationJobId); }
    public boolean isAlive(Long generationJobId) { return running.contains(generationJobId); }
}
