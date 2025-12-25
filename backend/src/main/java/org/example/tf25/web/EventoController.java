package org.example.tf25.web;

import org.example.tf25.domain.Evento;
import org.example.tf25.domain.EventoEstado;
import org.example.tf25.service.EventoService;
import org.example.tf25.service.SessionService;
import org.example.tf25.service.dto.AsientoDto;
import org.example.tf25.service.dto.SessionState;
import org.example.tf25.service.dto.RespuestaBloqueoAsientosDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final SessionService sessionService;

    public EventoController(EventoService eventoService, SessionService sessionService) {
        this.eventoService = eventoService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<Evento> listar(@RequestParam(value = "mostrarEliminados", defaultValue = "false") boolean mostrarEliminados) {
        if (mostrarEliminados) {
            return eventoService.findAll();
        }
        return eventoService.findByEstado(EventoEstado.ACTIVO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> obtener(@PathVariable Long id) {
        return eventoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
   //@PreAuthorize("hasAuthority('SCOPE_write') or hasRole('ADMIN')")
    public ResponseEntity<Evento> crear(@RequestBody Evento evento) {
        Evento saved = eventoService.save(evento);
        return ResponseEntity.created(URI.create("/api/eventos/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasAuthority('SCOPE_write') or hasRole('ADMIN')")
    public ResponseEntity<Evento> actualizar(@PathVariable Long id, @RequestBody Evento evento) {
        return eventoService.findById(id)
                .map(existing -> {
                    evento.setId(id);
                    return ResponseEntity.ok(eventoService.save(evento));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasAuthority('SCOPE_write') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (eventoService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        eventoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncEventos() {
        int count = eventoService.sincronizarEventos();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{externalId}/asientos")
    public ResponseEntity<List<AsientoDto>> obtenerAsientos(
            @PathVariable("externalId") String externalEventoId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        // 1) Resolver sesión
        SessionState sessionState = sessionService
                .obtenerSesion(sessionId)
                .orElseGet(() -> sessionService.crearNuevaSesionParaEvento(null, externalEventoId));

        if (sessionState.getExternalEventoId() == null) {
            sessionState.setExternalEventoId(externalEventoId);
            sessionService.guardarSesion(sessionState);
        }

        // 2) Consultar asientos al proxy
        var asientos = eventoService.obtenerAsientos(externalEventoId);

        // 3) Devolver asientos + header con X-Session-Id
        return ResponseEntity.ok()
                .header("X-Session-Id", sessionState.getSessionId())
                .body(asientos);
    }

    @PostMapping("/{externalId}/bloqueos")
    public ResponseEntity<?> bloquearAsientos(
            @PathVariable("externalId") String externalEventoId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestBody com.fasterxml.jackson.databind.JsonNode body
    ) {
        // 0) Normalizar cuerpo a List<String> asientosIds admitiendo dos formatos
        //    - ["r2c3","r2c4"]
        //    - { "asientosIds": ["r2c3","r2c4"] }
        java.util.List<String> asientosIds = extraerAsientosIds(body);
        if (asientosIds == null || asientosIds.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of(
                            "error", "BadRequest",
                            "mensaje", "Se requiere al menos un asiento en el body. Formato aceptado: [\"r2c3\",...] o {\"asientosIds\":[\"r2c3\",...]}"
                    )
            );
        }

        // 1) Resolver sesión (igual que en /asientos)
        SessionState sessionState = sessionService
                .obtenerSesion(sessionId)
                .orElseGet(() -> sessionService.crearNuevaSesionParaEvento(null, externalEventoId));

        if (sessionState.getExternalEventoId() == null) {
            sessionState.setExternalEventoId(externalEventoId);
            sessionService.guardarSesion(sessionState);
        } else if (!externalEventoId.equals(sessionState.getExternalEventoId())) {
            // Podés devolver error si la sesión apunta a otro evento
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // 2) Llamar al servicio para bloquear
        RespuestaBloqueoAsientosDto respuesta = eventoService.bloquearAsientosParaSesion(
                sessionState,
                asientosIds
        );

        // Guardar asientos seleccionados en sesión (mínimo para Issue #11)
        java.util.Set<String> exitosos = new java.util.HashSet<>();
        for (var r : respuesta.resultados()) {
            if (r.estado() != null && r.estado().equalsIgnoreCase("OK")) {
                exitosos.add(r.asientoId());
            }
        }
        boolean huboBloqueosReales = !exitosos.isEmpty();

        // Si no hay "BLOQUEADO" (por ejemplo la cátedra tira 500), igual guardamos los pedidos
        // para poder confirmar y que el flujo deje "PENDIENTE" (según consigna)
        if (exitosos.isEmpty()) {
            exitosos.addAll(asientosIds);
        }

        sessionState.setAsientosSeleccionados(exitosos);
        sessionState.setTuvoBloqueosExitosos(huboBloqueosReales);
        sessionState.setPasoActual(org.example.tf25.service.dto.PasoFlujoCompra.CONFIRMACION);
        sessionService.guardarSesion(sessionState);

        return ResponseEntity.ok()
                .header("X-Session-Id", sessionState.getSessionId())
                .body(respuesta);
    }

    /**
     * Extrae los asientosIds desde un body que puede ser un array plano o un objeto con campo "asientosIds".
     */
    private java.util.List<String> extraerAsientosIds(com.fasterxml.jackson.databind.JsonNode body) {
        if (body == null || body.isNull()) {
            return java.util.List.of();
        }
        com.fasterxml.jackson.databind.node.ArrayNode arrayNode = null;
        if (body.isArray()) {
            arrayNode = (com.fasterxml.jackson.databind.node.ArrayNode) body;
        } else if (body.has("asientosIds") && body.get("asientosIds").isArray()) {
            arrayNode = (com.fasterxml.jackson.databind.node.ArrayNode) body.get("asientosIds");
        }
        if (arrayNode == null) {
            return java.util.List.of();
        }
        java.util.List<String> result = new java.util.ArrayList<>();
        arrayNode.forEach(node -> {
            if (node.isTextual()) {
                result.add(node.asText());
            }
        });
        return result;
    }
}
