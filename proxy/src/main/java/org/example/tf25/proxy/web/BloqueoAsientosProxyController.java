package org.example.tf25.proxy.web;

import org.example.tf25.proxy.dto.PeticionBloqueoAsientosRemotaDto;
import org.example.tf25.proxy.dto.RespuestaBloqueoAsientosRemotaDto;
import org.example.tf25.proxy.service.BloqueoAsientosProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/endpoints/v1")
public class BloqueoAsientosProxyController {

    private static final Logger log = LoggerFactory.getLogger(BloqueoAsientosProxyController.class);

    private final BloqueoAsientosProxyService bloqueoAsientosProxyService;

    public BloqueoAsientosProxyController(BloqueoAsientosProxyService bloqueoAsientosProxyService) {
        this.bloqueoAsientosProxyService = bloqueoAsientosProxyService;
    }

    @PostMapping("/bloquear-asientos")
    public ResponseEntity<RespuestaBloqueoAsientosRemotaDto> bloquearAsientos(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionIdHeader,
            @RequestBody PeticionBloqueoAsientosRemotaDto peticion
    ) {
        String sid = (sessionIdHeader != null && !sessionIdHeader.isBlank())
                ? sessionIdHeader
                : peticion.sessionId();
        if (sid == null || sid.isBlank()) {
            // Fallback: nunca devolver sessionId null
            sid = java.util.UUID.randomUUID().toString();
            log.warn("Proxy: X-Session-Id ausente; generando sid={} para bloquear asientos", sid);
        }

        // Re-armar DTO con el sid priorizado
        PeticionBloqueoAsientosRemotaDto req = new PeticionBloqueoAsientosRemotaDto(
                peticion.externalEventoId(), sid, peticion.asientosIds()
        );

        var resp = bloqueoAsientosProxyService.bloquearAsientos(req);
        var respConSid = new RespuestaBloqueoAsientosRemotaDto(
                resp.externalEventoId(), sid, resp.resultados()
        );

        return ResponseEntity.ok()
                .header("X-Session-Id", sid)
                .body(respConSid);
    }
}
