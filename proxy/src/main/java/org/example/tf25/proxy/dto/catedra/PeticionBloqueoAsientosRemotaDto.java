package org.example.tf25.proxy.dto.catedra;

import java.util.List;

public record PeticionBloqueoAsientosRemotaDto(
        int eventoId,
        List<AsientoPosicionRemota> asientos
) {}
