package org.example.tf25.repository;

import org.example.tf25.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    Optional<Evento> findByExternalId(String externalId);
}
