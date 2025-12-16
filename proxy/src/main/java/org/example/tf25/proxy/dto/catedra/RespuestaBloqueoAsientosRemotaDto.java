package org.example.tf25.proxy.dto.catedra;

import java.util.List;

public record RespuestaBloqueoAsientosRemotaDto(
        boolean resultado,
        String descripcion,
        int eventoId,
        List<AsientoBloqueadoRemoto> asientos
) {}
