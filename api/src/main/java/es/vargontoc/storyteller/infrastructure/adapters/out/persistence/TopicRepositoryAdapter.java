package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.TopicRepository;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.infrastructure.mappers.TopicMapper;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaRepository;

@Repository
public class TopicRepositoryAdapter implements TopicRepository {

    private final TopicMapper mapper;
    private final TopicJpaRepository repository;

    public TopicRepositoryAdapter(TopicMapper mapper, TopicJpaRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Override
    public List<Topic> getTopics() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::toModel).toList();
    }

}
