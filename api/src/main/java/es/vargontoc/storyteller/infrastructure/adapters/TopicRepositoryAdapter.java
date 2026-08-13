package es.vargontoc.storyteller.infrastructure.adapters;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.domain.Topic;
import es.vargontoc.storyteller.infrastructure.mappers.TopicMapper;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaRepository;
import es.vargontoc.storyteller.ports.out.TopicRepository;

@Repository
public class TopicRepositoryAdapter implements TopicRepository {

    private final TopicMapper mapper;
    private final TopicJpaRepository repository;

    public TopicRepositoryAdapter(TopicMapper mapper, TopicJpaRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Override
    public Topic createTopic(Topic topic) {
        TopicJpaEntity entity = mapper.toEntity(topic);
        entity.setCreatedAt(LocalDateTime.now());

        return mapper.toModel(repository.save(entity));
    }

    @Override
    public List<Topic> getTopics() {
        return repository.findAll().stream().map(mapper::toModel).toList();
    }
    
}
