package org.example.tf25.proxy.dto;

import java.util.List;

/**
 * Petición tal como se envía a la cátedra.
 * Ajustá nombres/campos al contrato real si es distinto.
 */
public record PeticionBloqueoAsientosRemotaDto(
        String externalEventoId,
        String sessionId,
        List<String> asientosIds
) {}