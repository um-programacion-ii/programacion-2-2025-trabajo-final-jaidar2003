package org.example.tf25.service.dto;

/**
 * Resultado de intentar bloquear un asiento puntual.
 */
public record ResultadoBloqueoAsientoDto(
        String asientoId,
        String estado,   // "BLOQUEADO", "OCUPADO", "YA_BLOQUEADO_OTRO", etc.
        String mensaje   // detalle opcional
) {}