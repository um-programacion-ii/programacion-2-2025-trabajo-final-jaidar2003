package org.example.tf25.application.dto;

import java.util.List;

public record RespuestaBloqueoAsientosDto(
        String externalEventoId,
        String sessionId,
        List<ResultadoBloqueoAsientoDto> resultados
) {}