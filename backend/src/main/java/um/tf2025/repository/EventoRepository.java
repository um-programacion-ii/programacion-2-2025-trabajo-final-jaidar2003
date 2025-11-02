package um.tf2025.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import um.tf2025.domain.Evento;

/**
 * Spring Data JPA repository for the Evento entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {}
