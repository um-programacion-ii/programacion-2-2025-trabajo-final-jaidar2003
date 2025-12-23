package org.example.tf25.proxy.dto.catedra;

/**
 * Representa un asiento en el payload Redis de la cátedra para la clave evento_{id}.
 * Ejemplo de item:
 * {"fila":1,"columna":2,"estado":"Bloqueado","expira":"2025-12-21T18:51:33.314963308Z"}
 */
public record CatedraRedisAsientoDto(
        Integer fila,
        Integer columna,
        String estado,
        String expira
) {}
