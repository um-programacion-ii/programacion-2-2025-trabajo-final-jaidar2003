package org.example.tf25.web;

import org.example.tf25.domain.Venta;
import org.example.tf25.service.VentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    public record CrearVentaRequest(Long eventoId,
                                    String compradorEmail,
                                    Integer cantidad) {}

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_write') or hasRole('ADMIN')")
    public ResponseEntity<Venta> crear(@RequestBody CrearVentaRequest req) {
        Venta v = ventaService.crearVenta(req.eventoId(), req.compradorEmail(), req.cantidad());
        return ResponseEntity.created(URI.create("/api/ventas/" + v.getId())).body(v);
    }

    @GetMapping
    public List<Venta> listarPorEvento(@RequestParam("eventoId") Long eventoId) {
        return ventaService.listarPorEvento(eventoId);
    }
}
