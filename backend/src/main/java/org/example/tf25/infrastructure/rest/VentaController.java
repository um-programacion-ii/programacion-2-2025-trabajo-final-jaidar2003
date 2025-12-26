package org.example.tf25.infrastructure.rest;

import org.example.tf25.domain.model.Venta;
import org.example.tf25.application.usecase.VentaService;
import org.example.tf25.application.dto.VentaDto;
import org.example.tf25.infrastructure.persistence.VentaRetryJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final VentaRetryJob ventaRetryJob;

    public VentaController(VentaService ventaService, VentaRetryJob ventaRetryJob) {
        this.ventaService = ventaService;
        this.ventaRetryJob = ventaRetryJob;
    }

    public record CrearVentaRequest(Long eventoId,
                                    String compradorEmail,
                                    Integer cantidad) {}

    @PostMapping
    public ResponseEntity<VentaDto> crear(@RequestBody CrearVentaRequest req) {
        Venta v = ventaService.crearVenta(req.eventoId(), req.compradorEmail(), req.cantidad());
        return ResponseEntity.created(URI.create("/api/ventas/" + v.getId())).body(VentaDto.from(v));
    }

    @GetMapping
    public List<VentaDto> listar(
            @RequestParam(value = "eventoId", required = false) Long eventoId,
            @RequestParam(value = "email", required = false) String email
    ) {
        return ventaService.listarEntidades(eventoId, email).stream()
                .map(this::toDto)
                .toList();
    }

    private VentaDto toDto(Venta v) {
        return VentaDto.from(v); }

    public record VentaPendienteDto(Long id, String externalEventoId, String sessionId,
                                    String estado, Integer intentos, String ultimoError, java.time.Instant nextRetryAt) {}

    @GetMapping("/pendientes")
    public List<VentaPendienteDto> listarPendientes(@RequestParam(name = "limit", defaultValue = "50") Integer limit) {
        int lim = (limit == null) ? 50 : limit;
        return ventaService.listarPendientes(lim).stream()
                .map(v -> new VentaPendienteDto(
                        v.getId(), v.getExternalEventoId() != null ? v.getExternalEventoId() : "", v.getSessionId(),
                        v.getEstado() == null ? null : v.getEstado().name(),
                        v.getIntentosNotificacion(), v.getUltimoError(), v.getNextRetryAt()
                ))
                .toList();
    }

    public record ConfirmarVentaRequest(String compradorEmail, List<String> nombresOcupantes) {}

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
        List<String> ocupantes = (req != null) ? req.nombresOcupantes() : null;

        Venta v = ventaService.confirmarVentaDesdeSesion(sessionId, email, ocupantes);

        String msg = v.getEstado() == org.example.tf25.domain.model.VentaEstado.CONFIRMADA
                ? "Venta confirmada y notificada a la cátedra"
                : "Venta guardada localmente; notificación falló y quedó " + v.getEstado();

        return ResponseEntity.ok(new ConfirmarVentaResponse(
                v.getId(),
                v.getExternalEventoId() != null ? v.getExternalEventoId() : "",
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
