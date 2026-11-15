package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;
import static org.junit.jupiter.api.Assertions.*; import java.util.List;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest; import org.springframework.boot.test.context.TestConfiguration; import org.springframework.context.annotation.Import;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import jakarta.persistence.EntityManager;
import es.vargontoc.storyteller.domain.model.CatalogCharacter; import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
@DataJpaTest @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) @Import({CatalogCharacterRepositoryAdapter.class, CatalogCharacterRepositoryAdapterTest.JpaAuditingTestConfiguration.class}) class CatalogCharacterRepositoryAdapterTest {
 @Autowired CatalogCharacterRepositoryAdapter adapter; @Autowired CatalogCharacterJpaRepository jpa;
 @Autowired EntityManager entityManager;
 @Test void persistsAndReadsRequiredFields(){var s=adapter.create(new CatalogCharacter(null,"Luna","curiosa","zorra","fox",List.of("orange")));assertNotNull(s.id());entityManager.flush();entityManager.clear();var e=jpa.findById(s.id()).orElseThrow();assertEquals("Luna",e.getName());assertEquals("curiosa",e.getNarrativeDescription());assertEquals("zorra",e.getVisualDescription());assertEquals("fox",e.getMetadata().translate());assertEquals(List.of("orange"),e.getMetadata().attributes());assertNotNull(e.getCreatedAt());assertNotNull(e.getUpdatedAt());}
 @TestConfiguration(proxyBeanMethods = false) @EnableJpaAuditing static class JpaAuditingTestConfiguration {}
}
