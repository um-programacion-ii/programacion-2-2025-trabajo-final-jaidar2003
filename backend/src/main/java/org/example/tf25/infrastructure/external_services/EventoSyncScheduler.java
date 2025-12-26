package org.example.tf25.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventoSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(EventoSyncScheduler.class);

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
