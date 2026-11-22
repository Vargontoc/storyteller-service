package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceJpaRepository extends JpaRepository<VoiceJpaEntity, Long> {

    List<VoiceJpaEntity> findAllByOrderByIdAsc();

    Optional<VoiceJpaEntity> findByName(String name);
}
