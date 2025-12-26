package org.example.tf25.application.dto;

import java.util.List;

/**
 * DTO que representa la petición de bloqueo de asientos
 * desde el backend hacia el Proxy / cátedra.
 */
public record PeticionBloqueoAsientosDto(
        String externalEventoId,
        String sessionId,
        List<String> asientosIds
) {}