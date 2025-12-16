package org.example.tf25.proxy.dto;

import java.util.List;

public record RespuestaBloqueoAsientosRemotaDto(
        String externalEventoId,
        String sessionId,
        List<ResultadoBloqueoAsientoRemotoDto> resultados
) {}