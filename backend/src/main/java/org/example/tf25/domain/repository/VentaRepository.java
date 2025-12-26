package org.example.tf25.domain.repository;

import org.example.tf25.domain.model.Venta;
import org.example.tf25.domain.model.VentaEstado;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByEvento_Id(Long eventoId);

    List<Venta> findByCompradorEmailIgnoreCase(String email);

    @Override
    List<Venta> findAll();

    List<Venta> findTop50ByEstadoAndNextRetryAtBeforeOrderByNextRetryAtAsc(VentaEstado estado, Instant now);

    Optional<Venta> findFirstBySessionIdAndExternalEventoIdOrderByCreatedAtDesc(String sessionId, String externalEventoId);

    List<Venta> findByEstadoOrderByNextRetryAtAsc(VentaEstado estado, Pageable pageable);
}
