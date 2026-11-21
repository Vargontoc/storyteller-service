package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.infrastructure.mappers.PageVersionMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageVersionJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageVersionJpaRepository;

@Repository
public class StoryPageVersionRepositoryAdapter implements StoryPageVersionRepository {

    private final StoryPageVersionJpaRepository repository;
    private final StoryPageJpaRepository pageRepository;
    private final PageVersionMapper mapper;

    public StoryPageVersionRepositoryAdapter(
        StoryPageVersionJpaRepository repository,
        StoryPageJpaRepository pageRepository,
        PageVersionMapper mapper) {
        this.repository = repository;
        this.pageRepository = pageRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PageVersion add(Long pageId, PageVersion version) {
        StoryPageVersionJpaEntity entity = mapper.toEntity(version);
        entity.setPage(pageRepository.findById(pageId).orElseThrow());
        entity.setCreatedAt(LocalDateTime.now());
        if (version.isCurrent())
            repository.clearCurrent(pageId);
        return mapper.toModel(repository.save(entity));
    }

    @Override
    public List<PageVersion> findByPage(Long pageId) {
        return mapper.toModel(repository.findByPageIdOrderByIdAsc(pageId));
    }

    @Override
    @Transactional
    public void setCurrent(Long pageId, Long versionId) {
        repository.clearCurrent(pageId);
        if (repository.markCurrent(pageId, versionId) == 0)
            throw new IllegalArgumentException("La version " + versionId + " no pertenece a la pagina " + pageId);
    }

    @Override
    @Transactional
    public void deleteByPage(Long pageId) {
        repository.deleteByPageId(pageId);
    }
}
