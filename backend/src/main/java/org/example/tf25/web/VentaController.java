package org.example.tf25.web;

import org.example.tf25.domain.Venta;
import org.example.tf25.service.VentaService;
import org.example.tf25.service.dto.VentaDTO;
import org.example.tf25.web.dto.VentaDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final org.example.tf25.service.VentaRetryJob ventaRetryJob;

    public VentaController(VentaService ventaService, org.example.tf25.service.VentaRetryJob ventaRetryJob) {
        this.ventaService = ventaService;
        this.ventaRetryJob = ventaRetryJob;
    }

    public record CrearVentaRequest(Long eventoId,
                                    String compradorEmail,
                                    Integer cantidad) {}

    @PostMapping
    public ResponseEntity<VentaDTO> crear(@RequestBody CrearVentaRequest req) {
        Venta v = ventaService.crearVenta(req.eventoId(), req.compradorEmail(), req.cantidad());
        return ResponseEntity.created(URI.create("/api/ventas/" + v.getId())).body(VentaDTO.from(v));
    }

    @GetMapping
    public List<VentaDto> listarPorEvento(@RequestParam(value = "eventoId", required = false) Long eventoId) {
        return ventaService.listarEntidadesPorEvento(eventoId).stream()
                .map(this::toDto)
                .toList();
    }

    private VentaDto toDto(Venta v) {
        String externalEventId = v.getExternalEventoId();
        if (externalEventId == null || externalEventId.isBlank()) {
            externalEventId = (v.getEvento() != null && v.getEvento().getExternalId() != null)
                    ? v.getEvento().getExternalId()
                    : "";
        }
        java.util.List<String> asientos = v.getAsientosIds() == null
                ? java.util.List.of()
                : new java.util.ArrayList<>(v.getAsientosIds());
        String eventoNombre = (v.getEvento() != null && v.getEvento().getNombre() != null)
                ? v.getEvento().getNombre()
                : "";
        String estadoStr = (v.getEstado() != null) ? v.getEstado().name() : "";
        return new VentaDto(
                v.getId(),
                externalEventId,
                v.getCompradorEmail(),
                estadoStr,
                asientos,
                eventoNombre
        );
    }

    public record VentaPendienteDto(Long id, String externalEventoId, String sessionId,
                                    String estado, Integer intentos, String ultimoError, java.time.Instant nextRetryAt) {}

    @GetMapping("/pendientes")
    public List<VentaPendienteDto> listarPendientes(@RequestParam(name = "limit", defaultValue = "50") Integer limit) {
        int lim = (limit == null) ? 50 : limit;
        return ventaService.listarPendientes(lim).stream()
                .map(v -> new VentaPendienteDto(
                        v.getId(), v.getExternalEventoId(), v.getSessionId(),
                        v.getEstado() == null ? null : v.getEstado().name(),
                        v.getIntentosNotificacion(), v.getUltimoError(), v.getNextRetryAt()
                ))
                .toList();
    }

    public record ConfirmarVentaRequest(String compradorEmail) {}

    public record ConfirmarVentaResponse(
            Long ventaId,
            String externalEventoId,
            String sessionId,
            String estado,
            String mensaje
    ) {}

    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmarVentaResponse> confirmar(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody(required = false) ConfirmarVentaRequest req
    ) {
        String email = (req != null) ? req.compradorEmail() : null;

        Venta v = ventaService.confirmarVentaDesdeSesion(sessionId, email);

        String msg = v.getEstado() == org.example.tf25.domain.VentaEstado.CONFIRMADA
                ? "Venta confirmada y notificada a la cátedra"
                : "Venta guardada localmente; notificación falló y quedó " + v.getEstado();

        return ResponseEntity.ok(new ConfirmarVentaResponse(
                v.getId(),
                v.getExternalEventoId(),
                v.getSessionId(),
                v.getEstado().name(),
                msg
        ));
    }

    @PostMapping("/reintentar-pendientes")
    public ResponseEntity<String> reintentar() {
        ventaRetryJob.reintentarPendientes();
        return ResponseEntity.ok("Proceso de reintento disparado manualmente");
    }
}
