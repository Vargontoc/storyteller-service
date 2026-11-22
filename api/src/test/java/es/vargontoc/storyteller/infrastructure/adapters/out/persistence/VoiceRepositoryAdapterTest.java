package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.infrastructure.mappers.VoiceMapper;
import es.vargontoc.storyteller.infrastructure.persistence.VoiceJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.VoiceJpaRepository;

/** El repositorio JPA se simula con una lista en memoria que imita sus consultas y la restriccion unica del nombre. */
class VoiceRepositoryAdapterTest {

    private final List<VoiceJpaEntity> store = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();
    private final VoiceJpaRepository repository = mock(VoiceJpaRepository.class);
    private final VoiceRepositoryAdapter adapter = new VoiceRepositoryAdapter(repository, new VoiceMapper());

    VoiceRepositoryAdapterTest() {
        when(repository.saveAndFlush(any(VoiceJpaEntity.class))).thenAnswer(call -> {
            VoiceJpaEntity entity = call.getArgument(0);
            if (store.stream().anyMatch(v -> v.getName().equals(entity.getName())))
                throw new DataIntegrityViolationException("uq_voice_name");
            entity.setId(ids.incrementAndGet());
            store.add(entity);
            return entity;
        });
        when(repository.findAllByOrderByIdAsc()).thenAnswer(call -> store.stream()
            .sorted((a, b) -> a.getId().compareTo(b.getId()))
            .toList());
        when(repository.findByName(anyString())).thenAnswer(call -> store.stream()
            .filter(v -> v.getName().equals(call.getArgument(0)))
            .findFirst());
        when(repository.findById(anyLong())).thenAnswer(call -> store.stream()
            .filter(v -> v.getId().equals(call.getArgument(0)))
            .findFirst());
        doAnswer(call -> {
            store.removeIf(v -> v.getId().equals(call.getArgument(0)));
            return null;
        }).when(repository).deleteById(anyLong());
    }

    @Test
    void savedVoiceIsListedWithItsThreeData() {
        adapter.save(new Voice("Abuela", "es", true));

        var voices = adapter.findAll();

        assertEquals(1, voices.size());
        assertEquals("Abuela", voices.get(0).getName());
        assertEquals("es", voices.get(0).getLanguage());
        assertTrue(voices.get(0).isPermissionDeclared());
    }

    @Test
    void secondVoiceWithTheSameNameIsRejectedAndTheFirstDoesNotChange() {
        adapter.save(new Voice("Abuela", "es", true));

        assertThrows(DataIntegrityViolationException.class, () -> adapter.save(new Voice("Abuela", "en", true)));

        var voices = adapter.findAll();
        assertEquals(1, voices.size());
        assertEquals("es", voices.get(0).getLanguage());
    }

    @Test
    void findByNameReturnsTheVoiceOrNothing() {
        adapter.save(new Voice("Abuela", "es", true));

        assertEquals("Abuela", adapter.findByName("Abuela").orElseThrow().getName());
        assertTrue(adapter.findByName("Nadie").isEmpty());
    }

    @Test
    void findByIdReturnsTheVoiceOrNothing() {
        Long id = adapter.save(new Voice("Abuela", "es", true)).getId();

        assertEquals("Abuela", adapter.findById(id).orElseThrow().getName());
        assertTrue(adapter.findById(99L).isEmpty());
    }

    @Test
    void deletedVoiceIsNoLongerListed() {
        Long id = adapter.save(new Voice("Abuela", "es", true)).getId();
        adapter.save(new Voice("Abuelo", "es", true));

        adapter.delete(id);

        var names = adapter.findAll().stream().map(Voice::getName).toList();
        assertEquals(List.of("Abuelo"), names);
        assertFalse(adapter.findByName("Abuela").isPresent());
    }
}
