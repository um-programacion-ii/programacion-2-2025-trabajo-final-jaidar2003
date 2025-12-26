package org.example.tf25.application.usecase;

import org.example.tf25.domain.model.Venta;
import org.example.tf25.domain.model.VentaEstado;
import org.example.tf25.domain.repository.VentaRepository;
import org.example.tf25.infrastructure.messaging.VentaKafkaProducer;
import org.example.tf25.infrastructure.persistence.VentaRetryJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VentaRetryJobTest {

    private VentaRepository ventaRepository;
    private VentaKafkaProducer ventaKafkaProducer;
    private VentaRetryJob ventaRetryJob;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        ventaKafkaProducer = mock(VentaKafkaProducer.class);
        ventaRetryJob = new VentaRetryJob(ventaRepository, ventaKafkaProducer);
    }

    @Test
    void reintentarPendientesExito() {
        // GIVEN
        Venta v = new Venta();
        v.setId(1L);
        v.setEstado(VentaEstado.PENDIENTE);
        v.setAsientosIds(Set.of("r1c1"));
        v.setExternalEventoId("100");

        when(ventaRepository.findTop50ByEstadoAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq(VentaEstado.PENDIENTE), any(Instant.class)))
                .thenReturn(List.of(v));
        when(ventaKafkaProducer.enviarNotificacionVenta(any(Venta.class))).thenReturn(CompletableFuture.completedFuture(null));

        // WHEN
        ventaRetryJob.reintentarPendientes();

        // THEN
        assertEquals(VentaEstado.CONFIRMADA, v.getEstado());
        assertEquals(1, v.getIntentosNotificacion());
        assertNull(v.getNextRetryAt());
        verify(ventaRepository).save(v);
    }

    @Test
    void reintentarPendientesFallaSiguePendiente() {
        // GIVEN
        Venta v = new Venta();
        v.setId(1L);
        v.setEstado(VentaEstado.PENDIENTE);
        v.setAsientosIds(Set.of("r1c1"));
        v.setExternalEventoId("100");
        v.setIntentosNotificacion(1);

        when(ventaRepository.findTop50ByEstadoAndNextRetryAtBeforeOrderByNextRetryAtAsc(eq(VentaEstado.PENDIENTE), any(Instant.class)))
                .thenReturn(List.of(v));
        
        CompletableFuture<Void> futureFalla = new CompletableFuture<>();
        futureFalla.completeExceptionally(new RuntimeException("Kafka down"));
        when(ventaKafkaProducer.enviarNotificacionVenta(any(Venta.class))).thenReturn(futureFalla);

        // WHEN
        ventaRetryJob.reintentarPendientes();

        // THEN
        assertEquals(VentaEstado.PENDIENTE, v.getEstado());
        assertEquals(2, v.getIntentosNotificacion());
        assertNotNull(v.getNextRetryAt());
    }
}
