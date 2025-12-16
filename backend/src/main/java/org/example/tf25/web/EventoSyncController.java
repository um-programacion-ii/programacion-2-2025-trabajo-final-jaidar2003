package org.example.tf25.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tf25.service.EventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoSyncController {

    private final EventoService eventoService;

    /**
     * Endpoint para que el Proxy notifique cambios de un evento.
     * El Proxy llamará algo como:
     *   POST /api/eventos/sync/{externalId}
     * donde {externalId} es el ID externo del evento.
     */
    @PostMapping("/sync/{externalId}")
    public ResponseEntity<Void> syncEventoDesdeProxy(@PathVariable("externalId") String externalId) {
        log.info("Recibida notificación de cambio de evento desde Proxy para externalId={}", externalId);
        int procesados = eventoService.sincronizarEventoPorExternalId(externalId);

        if (procesados == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }
}
