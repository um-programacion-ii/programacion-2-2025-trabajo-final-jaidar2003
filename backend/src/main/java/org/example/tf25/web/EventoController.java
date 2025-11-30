package org.example.tf25.web;

import org.example.tf25.domain.Evento;
import org.example.tf25.service.EventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public List<Evento> listar() {
        return eventoService.findAll();
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
}
