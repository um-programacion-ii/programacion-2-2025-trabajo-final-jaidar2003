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
@RequestMapping("/api/endpoints/v1")
public class EventoProxyController {

    private static final Logger log = LoggerFactory.getLogger(EventoProxyController.class);

    private final RestClient catedraRestClient;

    public EventoProxyController(@Qualifier("catedraRestClient") RestClient catedraRestClient) {
        this.catedraRestClient = catedraRestClient;
    }

    @GetMapping("/eventos-resumidos")
    public ResponseEntity<?> listarEventos() {
        log.info("Proxy: pidiendo eventos resumidos a la cátedra...");
        try {
            EventoRemotoDto[] eventos = catedraRestClient.get()
                    .uri("/api/endpoints/v1/eventos-resumidos")
                    .retrieve()
                    .body(EventoRemotoDto[].class);

            if (eventos == null) {
                return ResponseEntity.ok(new EventoRemotoDto[0]);
            }
            return ResponseEntity.ok(eventos);
        } catch (Exception ex) {
            log.warn("Proxy: error consultando eventos en cátedra: {}", ex.toString());
            return ResponseEntity.ok(new EventoRemotoDto[0]);
        }
    }

    @GetMapping("/evento/{externalId}")
    public ResponseEntity<?> obtenerEvento(@PathVariable("externalId") String externalId) {
        log.info("Proxy: pidiendo evento {} a la cátedra...", externalId);
        try {
            EventoRemotoDto evento = catedraRestClient.get()
                    .uri("/api/endpoints/v1/evento/{id}", externalId)
                    .retrieve()
                    .body(EventoRemotoDto.class);

            if (evento == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(evento);
        } catch (Exception ex) {
            log.warn("Proxy: error consultando evento {} en cátedra: {}", externalId, ex.toString());
            return ResponseEntity.notFound().build();
        }
    }
}
