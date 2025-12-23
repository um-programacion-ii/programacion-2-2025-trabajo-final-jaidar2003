package org.example.tf25.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import org.example.tf25.domain.VentaEstado;
import org.example.tf25.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class VentaRetryJob {

    private final VentaRepository ventaRepository;
    private final VentaKafkaProducer ventaKafkaProducer;

    private static final int MAX_REINTENTOS = 10;

    public VentaRetryJob(VentaRepository ventaRepository,
                         VentaKafkaProducer ventaKafkaProducer) {
        this.ventaRepository = ventaRepository;
        this.ventaKafkaProducer = ventaKafkaProducer;
    }

    @Scheduled(fixedDelay = 30_000)
    public void reintentarPendientes() {
        var now = Instant.now();
        var pendientes = ventaRepository
                .findTop50ByEstadoAndNextRetryAtBeforeOrderByNextRetryAtAsc(VentaEstado.PENDIENTE, now);

        if (pendientes.isEmpty()) return;

        log.info("Iniciando reintento de {} ventas pendientes", pendientes.size());

        for (var v : pendientes) {
            int intento = v.getIntentosNotificacion() + 1;
            v.setIntentosNotificacion(intento);
            try {
                log.info("Venta {}: reintentando notificación a cátedra vía Kafka (intento {})...", v.getId(), intento);
                ventaKafkaProducer.enviarNotificacionVenta(v).join();

                v.setEstado(VentaEstado.CONFIRMADA);
                v.setUltimoError(null);
                v.setNextRetryAt(null);
                log.info("Venta {} confirmada en retry", v.getId());

            } catch (Exception ex) {
                Throwable cause = (ex instanceof java.util.concurrent.CompletionException) ? ex.getCause() : ex;
                if (cause == null) cause = ex;
                v.setUltimoError(cause.getClass().getSimpleName() + ": " + (cause.getMessage() == null ? "" : cause.getMessage()));

                if (v.getIntentosNotificacion() >= MAX_REINTENTOS) {
                    v.setEstado(VentaEstado.ERROR);
                    v.setNextRetryAt(null);
                    log.error("Venta {} pasó a estado ERROR tras {} intentos", v.getId(), v.getIntentosNotificacion());
                } else {
                    v.setNextRetryAt(Instant.now().plus(Duration.ofSeconds(60)));
                    log.info("Venta {} sigue pendiente (retry {})", v.getId(), v.getIntentosNotificacion());
                }
            }
            ventaRepository.save(v);
        }
    }
}
