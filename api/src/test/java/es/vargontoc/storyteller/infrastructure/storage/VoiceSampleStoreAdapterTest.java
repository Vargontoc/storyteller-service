package es.vargontoc.storyteller.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VoiceSampleStoreAdapterTest {

    private static final long VOICE_ID = 987654321L;

    private final VoiceSampleStoreAdapter store = new VoiceSampleStoreAdapter(new ResourceStorageAdapter());

    @AfterEach
    void cleanUp() {
        store.delete(VOICE_ID);
    }

    @Test
    void savedSampleIsRetrievedIdentical() {
        byte[] sample = {1, 2, 3, 4, 5, 127, -128};

        store.save(VOICE_ID, sample);

        assertArrayEquals(sample, store.find(VOICE_ID).orElseThrow());
    }

    @Test
    void afterDeleteSampleDoesNotExist() {
        store.save(VOICE_ID, new byte[] {9, 9, 9});

        store.delete(VOICE_ID);

        assertTrue(store.find(VOICE_ID).isEmpty());
    }

    @Test
    void neverSavedSampleDoesNotExist() {
        assertTrue(store.find(VOICE_ID).isEmpty());
    }
}
