package org.example.tf25.proxy.dto.catedra;

import java.util.List;

/**
 * Payload raíz almacenado en Redis por la cátedra bajo la clave "evento_{id}".
 * Ejemplo:
 * {
 *   "eventoId": 2,
 *   "asientos": [ {"fila":1,"columna":2,"estado":"Bloqueado","expira":"..."}, ... ]
 * }
 */
public record CatedraRedisEventoDto(
        Integer eventoId,
        List<CatedraRedisAsientoDto> asientos
) {}
