package org.example.tf25.proxy.web;

import org.example.tf25.proxy.dto.AsientoRemotoDto;
import org.example.tf25.proxy.service.AsientosProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/endpoints/v1")
public class AsientosProxyController {

    private static final Logger log = LoggerFactory.getLogger(AsientosProxyController.class);

    private final AsientosProxyService asientosProxyService;

    public AsientosProxyController(AsientosProxyService asientosProxyService) {
        this.asientosProxyService = asientosProxyService;
    }

    @GetMapping("/asientos/{externalEventoId}")
    public ResponseEntity<List<AsientoRemotoDto>> listarAsientos(
            @PathVariable("externalEventoId") String externalEventoId
    ) {
        log.info("Proxy: consultando asientos para evento {}", externalEventoId);
        try {
            List<AsientoRemotoDto> asientos = asientosProxyService.obtenerAsientos(externalEventoId);
            return ResponseEntity.ok(asientos);
        } catch (Exception ex) {
            log.warn("Proxy: error consultando asientos para evento {}: {}", externalEventoId, ex.toString());
            // No propagamos 500; devolvemos lista vacía para que el backend maneje el flujo
            return ResponseEntity.ok(List.of());
        }
    }
}
