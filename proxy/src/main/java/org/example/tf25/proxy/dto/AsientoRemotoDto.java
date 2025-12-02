package org.example.tf25.proxy.dto;

public record AsientoRemotoDto(
        String id,
        int fila,
        int columna,
        String estado // p.ej. "LIBRE", "OCUPADO", "BLOQUEADO"
) {}
