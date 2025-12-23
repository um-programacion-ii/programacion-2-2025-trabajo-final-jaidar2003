package org.example.tf25.service.dto;

import org.example.tf25.domain.Venta;
import org.example.tf25.domain.VentaEstado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record VentaDTO(
    Long id,
    Long eventoId,
    List<String> asientosIds,
    VentaEstado estado,
    String compradorEmail,
    Integer cantidad,
    BigDecimal total,
    LocalDateTime fechaHora,
    Instant createdAt
) {
    public static VentaDTO from(Venta v) {
        return new VentaDTO(
            v.getId(),
            v.getEventoId(),
            new ArrayList<>(v.getAsientosIds()),
            v.getEstado(),
            v.getCompradorEmail(),
            v.getCantidad(),
            v.getTotal(),
            v.getFechaHora(),
            v.getCreatedAt()
        );
    }
}
