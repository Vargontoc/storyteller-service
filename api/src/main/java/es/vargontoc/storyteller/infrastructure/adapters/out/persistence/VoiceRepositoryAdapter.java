package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.vargontoc.storyteller.application.ports.out.persistence.VoiceRepository;
import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.infrastructure.mappers.VoiceMapper;
import es.vargontoc.storyteller.infrastructure.persistence.VoiceJpaRepository;

@Repository
public class VoiceRepositoryAdapter implements VoiceRepository {

    private final VoiceJpaRepository repository;
    private final VoiceMapper mapper;

    public VoiceRepositoryAdapter(VoiceJpaRepository repository, VoiceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Voice save(Voice voice) {
        return mapper.toModel(repository.saveAndFlush(mapper.toEntity(voice)));
    }

    @Override
    public List<Voice> findAll() {
        return mapper.toModel(repository.findAllByOrderByIdAsc());
    }

    @Override
    public Optional<Voice> findByName(String name) {
        return repository.findByName(name).map(mapper::toModel);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
