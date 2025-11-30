package org.example.tf25.proxy.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class EventoProxyController {

    public record EventoRemotoDto(
            Long id,
            String nombre,
            String descripcion,
            LocalDateTime fechaHora,
            Integer cupo,
            BigDecimal precio
    ) {}

    @GetMapping("/api/eventos")
    public List<EventoRemotoDto> listarTodos() {
        // Dummy por ahora, para probar integración.
        return List.of(
                new EventoRemotoDto(1L, "Proxy Evento 1", "Demo 1",
                        LocalDateTime.now().plusDays(1), 100, new BigDecimal("5000")),
                new EventoRemotoDto(2L, "Proxy Evento 2", "Demo 2",
                        LocalDateTime.now().plusDays(3), 200, new BigDecimal("8000"))
        );
    }

    @GetMapping("/api/eventos/{externalId}")
    public EventoRemotoDto obtenerPorExternalId(@PathVariable String externalId) {
        // Por ahora devolvemos uno dummy con el externalId.
        // Más adelante se va a consultar al Servicio de la Cátedra / Redis.
        return new EventoRemotoDto(
                Long.valueOf(externalId),
                "Proxy Evento " + externalId,
                "Detalle dummy para externalId=" + externalId,
                LocalDateTime.now().plusDays(2),
                150,
                new BigDecimal("6000")
        );
    }
}
