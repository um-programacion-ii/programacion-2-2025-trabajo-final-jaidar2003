package org.example.tf25.web.dto;

import java.util.List;

/**
 * DTO pensado para el listado que consume el mobile.
 * Campos y nombres alineados con KMP: externalEventId y asientos.
 */
public record VentaDto(
        Long id,
        String externalEventId,
        String compradorEmail,
        String estado,
        List<String> asientos,
        String eventoNombre
) {}
