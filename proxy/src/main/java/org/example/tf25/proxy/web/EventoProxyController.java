package org.example.tf25.proxy.web;

import org.example.tf25.proxy.service.dto.EventoRemotoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/eventos")
public class EventoProxyController {

    private static final Logger log = LoggerFactory.getLogger(EventoProxyController.class);

    private final RestClient catedraRestClient;

    public EventoProxyController(@Qualifier("catedraRestClient") RestClient catedraRestClient) {
        this.catedraRestClient = catedraRestClient;
    }

    @GetMapping
    public ResponseEntity<?> listarEventos() {
        log.info("Proxy: pidiendo eventos a la cátedra...");
        EventoRemotoDto[] eventos = catedraRestClient.get()
                .uri("/api/eventos")
                .retrieve()
                .body(EventoRemotoDto[].class);

        if (eventos == null) {
            return ResponseEntity.ok(new EventoRemotoDto[0]);
        }
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<?> obtenerEvento(@PathVariable String externalId) {
        log.info("Proxy: pidiendo evento {} a la cátedra...", externalId);
        EventoRemotoDto evento = catedraRestClient.get()
                .uri("/api/eventos/{id}", externalId)
                .retrieve()
                .body(EventoRemotoDto.class);

        if (evento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(evento);
    }
}
