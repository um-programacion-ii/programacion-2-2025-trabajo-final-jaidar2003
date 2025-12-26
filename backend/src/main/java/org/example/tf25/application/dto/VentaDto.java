package org.example.tf25.application.dto;

import org.example.tf25.domain.model.Venta;
import org.example.tf25.domain.model.VentaEstado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record VentaDto(
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
    public static VentaDto from(Venta v) {
        return new VentaDto(
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
