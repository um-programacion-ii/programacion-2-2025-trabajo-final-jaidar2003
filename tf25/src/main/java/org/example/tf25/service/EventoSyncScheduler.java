package org.example.tf25.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoSyncScheduler {

    private final EventoService eventoService;

    /**
     * Ejecuta la sincronización automática de eventos.
     *
     * Se ejecuta cada tf25.sync.fixed-delay-ms milisegundos.
     * Por defecto: 60000 ms (1 minuto)
     */
    @Scheduled(fixedDelayString = "${tf25.sync.fixed-delay-ms:60000}")
    public void syncEventosPeriodicamente() {
        log.info("Iniciando sincronización automática de eventos...");
        int count = eventoService.sincronizarEventos();
        log.info("Sincronización completada: {} eventos procesados", count);
    }
}
