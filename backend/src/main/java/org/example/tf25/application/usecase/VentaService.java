package org.example.tf25.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.tf25.domain.model.Evento;
import org.example.tf25.domain.model.Venta;
import org.example.tf25.domain.repository.EventoRepository;
import org.example.tf25.domain.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.example.tf25.domain.model.VentaEstado;
import org.example.tf25.application.dto.SessionState;
import org.example.tf25.application.dto.VentaDto;
import org.example.tf25.infrastructure.messaging.VentaKafkaProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

@Service
@Transactional
public class VentaService {
    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    private final VentaRepository ventaRepository;
    private final EventoRepository eventoRepository;
    private final SessionService sessionService;
    private final RestClient proxyRestClient;
    private final VentaKafkaProducer ventaKafkaProducer;
    private final boolean exigirBloqueosEnConfirmacion;

    private static final int MAX_REINTENTOS = 10;

    public VentaService(VentaRepository ventaRepository,
                    EventoRepository eventoRepository,
                    SessionService sessionService,
                    RestClient restClient,
                    VentaKafkaProducer ventaKafkaProducer,
                    @Value("${tf25.venta.exigir-bloqueos:false}") boolean exigirBloqueosEnConfirmacion) {
        this.ventaRepository = ventaRepository;
        this.eventoRepository = eventoRepository;
        this.sessionService = sessionService;
        this.proxyRestClient = restClient;
        this.ventaKafkaProducer = ventaKafkaProducer;
        this.exigirBloqueosEnConfirmacion = exigirBloqueosEnConfirmacion;
    }

    public Venta crearVenta(Long eventoId, String compradorEmail, int cantidad) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new NoSuchElementException("Evento no encontrado: " + eventoId));

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (evento.getCupo() != null && evento.getCupo() < cantidad) {
            throw new IllegalStateException("No hay cupo suficiente para el evento");
        }

        // Calcular total
        BigDecimal precio = evento.getPrecio() == null ? BigDecimal.ZERO : evento.getPrecio();
        BigDecimal total = precio.multiply(BigDecimal.valueOf(cantidad));

        // Disminuir cupo
        if (evento.getCupo() != null) evento.setCupo(evento.getCupo() - cantidad);
        eventoRepository.save(evento);

        Venta v = new Venta();
        v.setEvento(evento);
        v.setCompradorEmail(compradorEmail);
        v.setCantidad(cantidad);
        v.setTotal(total);
        v.setFechaHora(LocalDateTime.now());
        return ventaRepository.save(v);
    }

    @Transactional(readOnly = true)
    public List<VentaDto> listarPorEvento(Long eventoId) {
        List<Venta> ventas = (eventoId != null)
                ? ventaRepository.findByEvento_Id(eventoId)
                : ventaRepository.findAll();

        return ventas.stream()
                .map(VentaDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Venta> listarEntidades(Long eventoId, String email) {
        if (email != null && !email.isBlank()) {
            return ventaRepository.findByCompradorEmailIgnoreCase(email);
        }
        return (eventoId != null)
                ? ventaRepository.findByEvento_Id(eventoId)
                : ventaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Venta> listarPendientes(int limit) {
        int lim = Math.max(1, Math.min(limit, 100));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, lim);
        return ventaRepository.findByEstadoOrderByNextRetryAtAsc(VentaEstado.PENDIENTE, pageable);
    }

    public Venta confirmarVentaDesdeSesion(String sessionId, String compradorEmail, List<String> nombresOcupantes) {
        SessionState sesion = sessionService.obtenerSesion(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada o inválida"));

        if (sesion.getExternalEventoId() == null || sesion.getExternalEventoId().isBlank()) {
            throw new IllegalArgumentException("La sesión no tiene externalEventoId");
        }
        if (sesion.getAsientosSeleccionados() == null || sesion.getAsientosSeleccionados().isEmpty()) {
            throw new IllegalArgumentException("La sesión no tiene asientos seleccionados");
        }
        // En perfil catedra exigimos que haya habido BLOQUEADO real
        if (exigirBloqueosEnConfirmacion && !sesion.isTuvoBloqueosExitosos()) {
            log.error("Fallo en confirmación: la sesión {} no tiene bloqueos exitosos registrados", sessionId);
            throw new IllegalArgumentException("No se puede confirmar la venta: los asientos no están bloqueados en el sistema de la cátedra. Por favor, vuelva a intentar la selección.");
        }

        String externalEventoId = sesion.getExternalEventoId();
        var asientos = new HashSet<>(sesion.getAsientosSeleccionados());

        // Buscar evento local por externalId (si existe)
        Evento evento = eventoRepository.findByExternalId(externalEventoId).orElse(null);

        int cantidad = asientos.size();
        BigDecimal precio = (evento != null && evento.getPrecio() != null) ? evento.getPrecio() : BigDecimal.ZERO;
        BigDecimal total = precio.multiply(BigDecimal.valueOf(cantidad));

        // Idempotencia: buscar venta existente por sessionId + externalEventoId
        var existenteOpt = ventaRepository
                .findFirstBySessionIdAndExternalEventoIdOrderByCreatedAtDesc(sessionId, externalEventoId);

        // 1) Si ya está CONFIRMADA, devolvemos tal cual (no tocar nada)
        if (existenteOpt.isPresent() && existenteOpt.get().getEstado() == VentaEstado.CONFIRMADA) {
            return existenteOpt.get();
        }

        Venta venta = existenteOpt.orElseGet(Venta::new);

        // 2) Si existe y está PENDIENTE, NO reescribimos asientos/cantidad/total; solo email si viene nuevo.
        //    Si es nueva, seteamos todos los campos.
        if (venta.getId() != null) {
            if (compradorEmail != null && !compradorEmail.isBlank()) {
                venta.setCompradorEmail(compradorEmail != null ? compradorEmail.trim() : null);
            }
            if (nombresOcupantes != null && !nombresOcupantes.isEmpty()) {
                venta.setNombresOcupantes(new java.util.ArrayList<>(nombresOcupantes));
            }
        } else {
            venta.setEvento(evento);
            venta.setExternalEventoId(externalEventoId);
            venta.setSessionId(sessionId);
            venta.setAsientosIds(asientos);
            venta.setCantidad(cantidad);
            venta.setTotal(total);
            if (compradorEmail != null) {
                venta.setCompradorEmail(compradorEmail != null ? compradorEmail.trim() : null);
            }
            if (nombresOcupantes != null) {
                venta.setNombresOcupantes(new java.util.ArrayList<>(nombresOcupantes));
            }
            venta.setFechaHora(LocalDateTime.now());
            venta.setEstado(VentaEstado.PENDIENTE);
            venta.setIntentosNotificacion(0);
        }

        // 1) Persistir local SIEMPRE (crear o actualizar)
        venta = ventaRepository.save(venta);

        // 2) Si ya estaba CONFIRMADA (raro), devolverla tal cual
        if (venta.getEstado() == VentaEstado.CONFIRMADA) {
            return venta;
        }

        // 3) Notificar a cátedra. Intentamos primero vía HTTP Proxy para que Catedra 
        // asocie la venta al usuario logueado (vía Token JWT que se propaga).
        int intento = venta.getIntentosNotificacion() + 1;
        venta.setIntentosNotificacion(intento);
        try {
            log.info("Venta {}: notificando a cátedra vía HTTP Proxy (intento {})...", venta.getId(), intento);
            
            var payload = JsonNodeFactory.instance.objectNode()
                    .put("ventaId", venta.getId())
                    .put("externalEventoId", externalEventoId)
                    .put("sessionId", sessionId)
                    .put("compradorEmail", venta.getCompradorEmail() != null ? venta.getCompradorEmail() : "");
            
            var seatsArr = JsonNodeFactory.instance.arrayNode();
            venta.getAsientosIds().forEach(seatsArr::add);
            payload.set("asientosIds", seatsArr);

            JsonNode resp = proxyRestClient.post()
                    .uri("/api/endpoints/v1/realizar-venta")
                    .header("X-Session-Id", sessionId)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            if (resp != null && resp.has("resultado") && resp.get("resultado").asBoolean()) {
                venta.setEstado(VentaEstado.CONFIRMADA);
                venta.setUltimoError(null);
                venta.setNextRetryAt(null);
                log.info("Venta {}: confirmada exitosamente vía HTTP Proxy", venta.getId());
                return ventaRepository.save(venta);
            }
            
            log.warn("Venta {}: HTTP Proxy no confirmó (resp={}), intentando Kafka como fallback...", venta.getId(), resp);
            ventaKafkaProducer.enviarNotificacionVenta(venta).join();

            venta.setEstado(VentaEstado.CONFIRMADA);
            venta.setUltimoError(null);
            venta.setNextRetryAt(null);
            log.info("Venta {}: confirmada vía Kafka (fallback)", venta.getId());
            return ventaRepository.save(venta);

        } catch (Exception ex) {
            Throwable cause = (ex instanceof java.util.concurrent.CompletionException) ? ex.getCause() : ex;
            if (cause == null) cause = ex;
            venta.setUltimoError(cause.getClass().getSimpleName() + ": " + (cause.getMessage() == null ? "" : cause.getMessage()));

            if (venta.getIntentosNotificacion() >= MAX_REINTENTOS) {
                venta.setEstado(VentaEstado.ERROR);
                venta.setNextRetryAt(null);
                log.error("Venta {}: falló notificación (intento {}) y pasó a ERROR", venta.getId() != null ? venta.getId() : "NUEVA", venta.getIntentosNotificacion());
            } else {
                venta.setEstado(VentaEstado.PENDIENTE);
                venta.setNextRetryAt(Instant.now().plus(Duration.ofSeconds(30)));
                log.warn("Venta {}: falló notificación (intento {}), queda PENDIENTE para reintento", venta.getId() != null ? venta.getId() : "NUEVA", venta.getIntentosNotificacion());
            }
            return ventaRepository.save(venta);
        }
    }
}
