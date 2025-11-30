package org.example.tf25.repository;

import org.example.tf25.domain.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByEvento_Id(Long eventoId);
}
