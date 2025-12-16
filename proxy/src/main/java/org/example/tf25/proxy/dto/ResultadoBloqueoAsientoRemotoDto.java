package org.example.tf25.proxy.dto;

public record ResultadoBloqueoAsientoRemotoDto(
        String asientoId,
        String estado,
        String mensaje
) {}