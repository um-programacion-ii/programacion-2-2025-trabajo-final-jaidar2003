package org.example.tf25.proxy.web;

import lombok.extern.slf4j.Slf4j;
import org.example.tf25.proxy.dto.PeticionBloqueoAsientosRemotaDto;
import org.example.tf25.proxy.dto.RespuestaBloqueoAsientosRemotaDto;
import org.example.tf25.proxy.service.BloqueoAsientosProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/asientos/bloqueos")
public class BloqueoAsientosProxyController {

    private final BloqueoAsientosProxyService bloqueoAsientosProxyService;

    public BloqueoAsientosProxyController(BloqueoAsientosProxyService bloqueoAsientosProxyService) {
        this.bloqueoAsientosProxyService = bloqueoAsientosProxyService;
    }

    @PostMapping
    public ResponseEntity<RespuestaBloqueoAsientosRemotaDto> bloquearAsientos(
            @RequestBody PeticionBloqueoAsientosRemotaDto peticion
    ) {
        var respuesta = bloqueoAsientosProxyService.bloquearAsientos(peticion);
        return ResponseEntity.ok(respuesta);
    }
}
