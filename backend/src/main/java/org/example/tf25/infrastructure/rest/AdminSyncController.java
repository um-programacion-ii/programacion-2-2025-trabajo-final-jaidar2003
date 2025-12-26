package org.example.tf25.infrastructure.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.tf25.application.usecase.EventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSyncController {
    private static final Logger log = LoggerFactory.getLogger(AdminSyncController.class);

    private final EventoService eventoService;

    /**
     * Endpoint manual para sincronizar todos los eventos desde el proxy.
     * Llama a EventoService.sincronizarEventos().
     */
    @PostMapping("/sync/eventos")
    public ResponseEntity<Map<String, Object>> syncEventos() {
        log.info("Solicitud manual de sincronización de eventos desde /api/admin/sync/eventos");

        int procesados = eventoService.sincronizarEventos();

        return ResponseEntity.ok(
                Map.of(
                        "procesados", procesados
                )
        );
    }
}
