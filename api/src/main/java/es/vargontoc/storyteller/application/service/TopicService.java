package es.vargontoc.storyteller.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.persistence.TopicUseCase;
import es.vargontoc.storyteller.application.ports.out.persistence.TopicRepository;
import es.vargontoc.storyteller.domain.model.Topic;
import jakarta.transaction.Transactional;

/**
 * Consulta el catálogo interno de temas. El catálogo es fijo y se carga por
 * migración; no hay generación de temas.
 */
@Service
@Transactional
public class TopicService implements TopicUseCase {

    private final TopicRepository repository;

    public TopicService(TopicRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Topic> getTopics() {
        return repository.getTopics();
    }

}
