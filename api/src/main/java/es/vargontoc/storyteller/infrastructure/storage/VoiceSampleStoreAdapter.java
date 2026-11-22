package es.vargontoc.storyteller.infrastructure.storage;

import java.util.Optional;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.VoiceSampleStore;

@Component
public class VoiceSampleStoreAdapter implements VoiceSampleStore {

    private final ResourceStorage storage;

    public VoiceSampleStoreAdapter(ResourceStorage storage) {
        this.storage = storage;
    }

    @Override
    public void save(long voiceId, byte[] sample) {
        storage.saveVoiceSample(voiceId, sample);
    }

    @Override
    public Optional<byte[]> find(long voiceId) {
        return Optional.ofNullable(storage.getVoiceSample(voiceId));
    }

    @Override
    public void delete(long voiceId) {
        storage.deleteVoiceSample(voiceId);
    }
}
