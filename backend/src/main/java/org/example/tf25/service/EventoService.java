package org.example.tf25.service;

import lombok.extern.slf4j.Slf4j;
import org.example.tf25.domain.Evento;
import org.example.tf25.domain.EventoEstado;
import org.example.tf25.repository.EventoRepository;
import org.example.tf25.service.dto.AsientoDto;
import org.example.tf25.service.dto.PeticionBloqueoAsientosDto;
import org.example.tf25.service.dto.RespuestaBloqueoAsientosDto;
import org.example.tf25.service.dto.ResultadoBloqueoAsientoDto;
import org.example.tf25.service.dto.SessionState;
import org.example.tf25.proxy.dto.EventoProxyDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
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
            RestClient restClient
    ) {
        this.eventoRepository = eventoRepository;
        this.restClient = restClient;
    }

    @Transactional(readOnly = true)
    public List<Evento> findAll() {
        return eventoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Evento> findByEstado(EventoEstado estado) {
        return eventoRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public Optional<Evento> findById(Long id) {
        return eventoRepository.findById(id);
    }

    public Evento save(Evento evento) {
        return eventoRepository.save(evento);
    }

    public void delete(Long id) {
        eventoRepository.findById(id).ifPresent(evento -> {
            evento.setEstado(EventoEstado.ELIMINADO);
            eventoRepository.save(evento);
        });
    }

    /**
     * Sincroniza eventos desde el proxy de la cátedra.
     * - Si el evento ya existe (mismo externalId), se actualiza.
     * - Si no existe, se crea.
     * Devuelve cuántos eventos se procesaron.
     */
    public int sincronizarEventos() {
        try {
            EventoProxyDto[] remotos = restClient.get()
                    .uri("/api/endpoints/v1/eventos-resumidos")
                    .retrieve()
                    .body(EventoProxyDto[].class);

            if (remotos == null) {
                log.info("Sincronización de eventos: no se recibieron eventos remotos");
                return 0;
            }

            // 1. Guardar la lista de IDs externos que siguen vivos en la cátedra
            List<String> idsRemotos = Arrays.stream(remotos)
                    .filter(d -> d.getId() != null)
                    .map(d -> d.getId().toString())
                    .toList();

            int count = 0;
            for (EventoProxyDto dto : remotos) {
                String externalId = dto.getId() != null ? dto.getId().toString() : null;

                Evento evento = externalId != null
                        ? eventoRepository.findByExternalId(externalId).orElseGet(Evento::new)
                        : new Evento();

                evento.setExternalId(externalId);
                evento.setNombre(dto.getNombre());
                evento.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion() : dto.getResumen());
                if (dto.getFechaHora() != null) {
                    LocalDateTime fechaLocal = LocalDateTime.ofInstant(
                            dto.getFechaHora(),
                            ZoneId.of("America/Argentina/Mendoza")
                    );
                    evento.setFechaHora(fechaLocal);
                }
                evento.setCupo(dto.getCupo() != null ? dto.getCupo() : 0);
                evento.setPrecio(dto.getPrecio());
                evento.setEstado(EventoEstado.ACTIVO); // Si vino en la lista, está activo

                eventoRepository.save(evento);
                count++;
            }

            // 2. "Soft Delete": Marcar como ELIMINADO lo que tenemos local pero no vino en remotos
            List<Evento> locales = eventoRepository.findAll();
            for (Evento local : locales) {
                if (local.getExternalId() != null && !idsRemotos.contains(local.getExternalId())) {
                    if (local.getEstado() != EventoEstado.ELIMINADO) {
                        local.setEstado(EventoEstado.ELIMINADO);
                        eventoRepository.save(local);
                        log.info("Evento {} marcado como ELIMINADO porque ya no existe en la cátedra", local.getExternalId());
                    }
                }
            }

            log.info("Sincronización de eventos completada: {} eventos procesados", count);
            return count;

        } catch (Exception ex) {
            log.warn("No se pudo sincronizar eventos desde el proxy", ex);
            return 0;
        }
    }

    /**
     * Sincroniza un evento individual desde el proxy por su externalId.
     * Retorna 1 si se procesó el evento, 0 si no se encontró o hubo error.
     */
    public int sincronizarEventoPorExternalId(String externalId) {
        try {
            EventoProxyDto dto = restClient.get()
                    .uri("/api/endpoints/v1/evento/{externalId}", externalId)
                    .retrieve()
                    .body(EventoProxyDto.class);

            if (dto == null) {
                log.warn("Sincronización individual: no se encontró evento con externalId={}", externalId);
                // No se encontró en la cátedra -> lo marcamos como eliminado localmente
                eventoRepository.findByExternalId(externalId).ifPresent(e -> {
                    if (e.getEstado() != EventoEstado.ELIMINADO) {
                        e.setEstado(EventoEstado.ELIMINADO);
                        eventoRepository.save(e);
                        log.info("Evento {} marcado como ELIMINADO localmente", externalId);
                    }
                });
                return 0;
            }

            Evento evento = eventoRepository.findByExternalId(externalId)
                    .orElseGet(Evento::new);

            evento.setExternalId(externalId);
            evento.setNombre(dto.getNombre());
            evento.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion() : dto.getResumen());
            if (dto.getFechaHora() != null) {
                LocalDateTime fechaLocal = LocalDateTime.ofInstant(
                        dto.getFechaHora(),
                        ZoneId.of("America/Argentina/Mendoza")
                );
                evento.setFechaHora(fechaLocal);
            }
            evento.setCupo(dto.getCupo() != null ? dto.getCupo() : 0);
            evento.setPrecio(dto.getPrecio());
            evento.setEstado(EventoEstado.ACTIVO);

            eventoRepository.save(evento);

            log.info("Sincronización individual completada para externalId={}", externalId);
            return 1;
        } catch (Exception ex) {
            log.warn("Error sincronizando evento por externalId={}", externalId, ex);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<AsientoDto> obtenerAsientos(String externalEventoId) {
        try {
            AsientoDto[] asientos = restClient.get()
                    .uri("/api/endpoints/v1/asientos/{externalEventoId}", externalEventoId)
                    .retrieve()
                    .body(AsientoDto[].class);

            if (asientos == null) {
                return List.of();
            }
            return Arrays.asList(asientos);
        } catch (Exception ex) {
            throw new RuntimeException("Error obteniendo asientos desde proxy: " + ex.getClass().getSimpleName(), ex);
        }
    }

    public RespuestaBloqueoAsientosDto bloquearAsientosParaSesion(
            SessionState sessionState,
            List<String> asientosIds
    ) {
        // Construir petición hacia el proxy
        PeticionBloqueoAsientosDto peticion = new PeticionBloqueoAsientosDto(
                sessionState.getExternalEventoId(),
                sessionState.getSessionId(),
                asientosIds
        );

        try {
            // Llamar al proxy y evitar problemas de Content-Type (p.ej. application/octet-stream)
            ResponseEntity<byte[]> resp = restClient.post()
                    .uri("/api/endpoints/v1/bloquear-asientos")
                    .header("X-Session-Id", sessionState.getSessionId())
                    .body(peticion)
                    .retrieve()
                    .toEntity(byte[].class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                byte[] bodyBytes = resp.getBody();
                if (bodyBytes != null && bodyBytes.length > 0) {
                    try {
                        ObjectMapper om = new ObjectMapper()
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        RespuestaBloqueoAsientosDto parsed = om.readValue(bodyBytes, RespuestaBloqueoAsientosDto.class);
                        if (parsed != null) {
                            return parsed;
                        }
                    } catch (Exception parseEx) {
                        log.warn("Bloqueo: respuesta 2xx pero no se pudo parsear body ({}). Asumimos OK para asientos solicitados.", parseEx.getClass().getSimpleName());
                    }
                }
                // Sin body o no parseable: asumimos OK para permitir continuar según política cátedra
                return new RespuestaBloqueoAsientosDto(
                        sessionState.getExternalEventoId(),
                        sessionState.getSessionId(),
                        asientosIds.stream()
                                .map(id -> new ResultadoBloqueoAsientoDto(id, "OK", null))
                                .toList()
                );
            } else {
                // No 2xx: devolver errores por cada asiento
                return new RespuestaBloqueoAsientosDto(
                        sessionState.getExternalEventoId(),
                        sessionState.getSessionId(),
                        asientosIds.stream()
                                .map(id -> new ResultadoBloqueoAsientoDto(
                                        id,
                                        "ERROR",
                                        "Proxy devolvió status=" + resp.getStatusCode().value()
                                ))
                                .toList()
                );
            }
        } catch (Exception ex) {
            log.warn("No se pudo solicitar bloqueo de asientos al Proxy para externalId={} (session={}): {}",
                    sessionState.getExternalEventoId(), sessionState.getSessionId(), ex.toString());
            return new RespuestaBloqueoAsientosDto(
                    sessionState.getExternalEventoId(),
                    sessionState.getSessionId(),
                    asientosIds.stream()
                            .map(id -> new ResultadoBloqueoAsientoDto(
                                    id,
                                    "ERROR",
                                    "Error comunicándose con el Proxy: " + ex.getClass().getSimpleName()
                            ))
                            .toList()
            );
        }
    }

}
