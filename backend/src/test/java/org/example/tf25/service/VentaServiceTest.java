package org.example.tf25.service;

import org.example.tf25.domain.Evento;
import org.example.tf25.domain.Venta;
import org.example.tf25.domain.VentaEstado;
import org.example.tf25.repository.EventoRepository;
import org.example.tf25.repository.VentaRepository;
import org.example.tf25.service.dto.SessionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VentaServiceTest {

    private VentaRepository ventaRepository;
    private EventoRepository eventoRepository;
    private SessionService sessionService;
    private VentaKafkaProducer ventaKafkaProducer;
    private VentaService ventaService;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        eventoRepository = mock(EventoRepository.class);
        sessionService = mock(SessionService.class);
        ventaKafkaProducer = mock(VentaKafkaProducer.class);

        ventaService = new VentaService(
                ventaRepository,
                eventoRepository,
                sessionService,
                mock(RestClient.class),
                ventaKafkaProducer,
                false // exigirBloqueosEnConfirmacion
        );
    }

    @Test
    void confirmarVentaExito() {
        // GIVEN
        String sessionId = "s1";
        String externalId = "100";
        SessionState session = new SessionState(sessionId, "u1", externalId, Set.of("r1c1"), null);
        session.setTuvoBloqueosExitosos(true);

        Evento evento = new Evento();
        evento.setExternalId(externalId);
        evento.setPrecio(BigDecimal.valueOf(1000));

        when(sessionService.obtenerSesion(sessionId)).thenReturn(Optional.of(session));
        when(eventoRepository.findByExternalId(externalId)).thenReturn(Optional.of(evento));
        when(ventaRepository.findFirstBySessionIdAndExternalEventoIdOrderByCreatedAtDesc(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));
        when(ventaKafkaProducer.enviarNotificacionVenta(any(Venta.class))).thenReturn(CompletableFuture.completedFuture(null));

        // WHEN
        Venta v = ventaService.confirmarVentaDesdeSesion(sessionId, "test@test.com", java.util.List.of());

        // THEN
        assertEquals(VentaEstado.CONFIRMADA, v.getEstado());
        assertEquals(1, v.getIntentosNotificacion());
        verify(ventaKafkaProducer).enviarNotificacionVenta(any(Venta.class));
    }

    @Test
    void confirmarVentaFallaKafkaQuedaPendiente() {
        // GIVEN
        String sessionId = "s1";
        String externalId = "100";
        SessionState session = new SessionState(sessionId, "u1", externalId, Set.of("r1c1"), null);
        session.setTuvoBloqueosExitosos(true);

        when(sessionService.obtenerSesion(sessionId)).thenReturn(Optional.of(session));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));
        
        CompletableFuture<Void> futureFalla = new CompletableFuture<>();
        futureFalla.completeExceptionally(new RuntimeException("Kafka down"));
        when(ventaKafkaProducer.enviarNotificacionVenta(any(Venta.class))).thenReturn(futureFalla);

        // WHEN
        Venta v = ventaService.confirmarVentaDesdeSesion(sessionId, "test@test.com", java.util.List.of());

        // THEN
        assertEquals(VentaEstado.PENDIENTE, v.getEstado());
        assertEquals(1, v.getIntentosNotificacion());
        assertNotNull(v.getNextRetryAt());
    }

    @Test
    void confirmarVentaAgotaIntentosPasaAError() {
        // GIVEN
        String sessionId = "s1";
        String externalId = "100";
        SessionState session = new SessionState(sessionId, "u1", externalId, Set.of("r1c1"), null);
        session.setTuvoBloqueosExitosos(true);

        Venta ventaExistente = new Venta();
        ventaExistente.setId(1L);
        ventaExistente.setEstado(VentaEstado.PENDIENTE);
        ventaExistente.setIntentosNotificacion(9); // Próximo será el 10

        when(sessionService.obtenerSesion(sessionId)).thenReturn(Optional.of(session));
        when(ventaRepository.findFirstBySessionIdAndExternalEventoIdOrderByCreatedAtDesc(anyString(), anyString()))
                .thenReturn(Optional.of(ventaExistente));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<Void> futureFalla = new CompletableFuture<>();
        futureFalla.completeExceptionally(new RuntimeException("Kafka down"));
        when(ventaKafkaProducer.enviarNotificacionVenta(any(Venta.class))).thenReturn(futureFalla);

        // WHEN
        Venta v = ventaService.confirmarVentaDesdeSesion(sessionId, "test@test.com", java.util.List.of());

        // THEN
        assertEquals(VentaEstado.ERROR, v.getEstado());
        assertEquals(10, v.getIntentosNotificacion());
        assertNull(v.getNextRetryAt());
    }
}
