package org.example.tf25.infrastructure.rest;

import org.example.tf25.application.dto.EventoRemotoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class MockEventoController {

    @GetMapping("/api/mock-eventos")
    public List<EventoRemotoDto> mockEventos() {
        return List.of(
            new EventoRemotoDto(
                1L,
                "Show prueba",
                "Demo A",
                LocalDateTime.now().plusDays(1),
                100,
                new BigDecimal("5000")
            ),
            new EventoRemotoDto(
                2L,
                "Recital test",
                "Demo B",
                LocalDateTime.now().plusDays(3),
                150,
                new BigDecimal("7500")
            )
        );
    }
}