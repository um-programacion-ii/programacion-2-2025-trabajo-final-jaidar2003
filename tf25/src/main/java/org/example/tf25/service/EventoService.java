package org.example.tf25.service;

import lombok.extern.slf4j.Slf4j;
import org.example.tf25.domain.Evento;
import org.example.tf25.repository.EventoRepository;
import org.example.tf25.service.dto.EventoRemotoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class EventoService {

    private final EventoRepository eventoRepository;
    private final RestClient restClient;

    public EventoService(
            EventoRepository eventoRepository,
            RestClient.Builder restClientBuilder,
            @Value("${tf25.proxy.base-url:http://localhost:8081}") String proxyBaseUrl
    ) {
        this.eventoRepository = eventoRepository;
        this.restClient = restClientBuilder.baseUrl(proxyBaseUrl).build();
    }

    @Transactional(readOnly = true)
    public List<Evento> findAll() {
        return eventoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Evento> findById(Long id) {
        return eventoRepository.findById(id);
    }

    public Evento save(Evento evento) {
        return eventoRepository.save(evento);
    }

    public void delete(Long id) {
        eventoRepository.deleteById(id);
    }

    /**
     * Sincroniza eventos desde el proxy de la cátedra.
     * - Si el evento ya existe (mismo externalId), se actualiza.
     * - Si no existe, se crea.
     * Devuelve cuántos eventos se procesaron.
     */
    public int sincronizarEventos() {
        try {
            EventoRemotoDto[] remotos = restClient.get()
                    // TODO(issue-8): volver a usar el proxy real cuando exista
                    // Usamos path relativo para respetar el baseUrl (mock en dev)
                    .uri("/api/mock-eventos")
                    .retrieve()
                    .body(EventoRemotoDto[].class);

            if (remotos == null || remotos.length == 0) {
                log.info("Sincronización de eventos: no se recibieron eventos remotos");
                return 0;
            }

            int count = 0;
            for (EventoRemotoDto dto : remotos) {
                String externalId = dto.getId() != null ? dto.getId().toString() : null;

                Evento evento = externalId != null
                        ? eventoRepository.findByExternalId(externalId).orElseGet(Evento::new)
                        : new Evento();

                evento.setExternalId(externalId);
                evento.setNombre(dto.getNombre());
                evento.setDescripcion(dto.getDescripcion());
                evento.setFechaHora(dto.getFechaHora());
                evento.setCupo(dto.getCupo() != null ? dto.getCupo() : 0);
                evento.setPrecio(dto.getPrecio());

                eventoRepository.save(evento);
                count++;
            }

            log.info("Sincronización de eventos completada: {} eventos procesados", count);
            return count;

        } catch (Exception ex) {
            log.warn("No se pudo sincronizar eventos desde el mock", ex);
            return 0;
        }
    }
}
