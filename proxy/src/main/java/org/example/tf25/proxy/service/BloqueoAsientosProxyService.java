package org.example.tf25.proxy.service;

import org.example.tf25.proxy.dto.PeticionBloqueoAsientosRemotaDto;
import org.example.tf25.proxy.dto.RespuestaBloqueoAsientosRemotaDto;
import org.example.tf25.proxy.dto.ResultadoBloqueoAsientoRemotoDto;
import org.example.tf25.proxy.dto.catedra.AsientoBloqueadoRemoto;
import org.example.tf25.proxy.dto.catedra.AsientoPosicionRemota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BloqueoAsientosProxyService {

    private static final Logger log = LoggerFactory.getLogger(BloqueoAsientosProxyService.class);

    private static final Pattern ASIENTO_ID_PATTERN = Pattern.compile("r(\\d+)c(\\d+)", Pattern.CASE_INSENSITIVE);

    private final RestClient catedraRestClient;
    private final CatedraAuthService authService;
    private final LockService lockService;

    public BloqueoAsientosProxyService(@Qualifier("catedraRestClient") RestClient catedraRestClient,
                                       CatedraAuthService authService,
                                       LockService lockService) {
        this.catedraRestClient = catedraRestClient;
        this.authService = authService;
        this.lockService = lockService;
    }

    public RespuestaBloqueoAsientosRemotaDto bloquearAsientos(PeticionBloqueoAsientosRemotaDto peticion) {
        String externalEventoId = peticion.externalEventoId();
        String sessionId = peticion.sessionId();
        log.info("Proxy: solicitud de bloqueo: evento={}, session={}, asientos={}"
                , externalEventoId, sessionId, peticion.asientosIds());

        // 1) Validar externalEventoId numérico para la cátedra
        int eventoId;
        try {
            eventoId = Integer.parseInt(externalEventoId);
        } catch (NumberFormatException nfe) {
            log.warn("Proxy: externalEventoId={} no es numérico, no se puede invocar a la cátedra", externalEventoId);
            return new RespuestaBloqueoAsientosRemotaDto(
                    externalEventoId,
                    sessionId,
                    peticion.asientosIds().stream()
                            .map(id -> new ResultadoBloqueoAsientoRemotoDto(id, "ERROR", "externalEventoId no numérico"))
                            .toList()
            );
        }

        // 2) Parsear ids a posiciones válidas y pre-chequear conflictos locales (Redis)
        List<String> idsValidos = new ArrayList<>();
        List<AsientoPosicionRemota> posiciones = new ArrayList<>();
        List<ResultadoBloqueoAsientoRemotoDto> resultadosParciales = new ArrayList<>();
        Set<String> yaOkPorMismaSesion = new HashSet<>();

        for (String asientoId : peticion.asientosIds()) {
            var posOpt = parseAsientoId(asientoId);
            if (posOpt.isEmpty()) {
                resultadosParciales.add(new ResultadoBloqueoAsientoRemotoDto(asientoId, "ERROR", "Formato de asiento inválido"));
                continue;
            }

            // Pre-check de lock
            var ownerOpt = lockService.whoLocks(externalEventoId, asientoId);
            if (ownerOpt.isPresent()) {
                String owner = ownerOpt.get();
                if (!owner.equals(sessionId)) {
                    // conflicto con otra sesión
                    resultadosParciales.add(new ResultadoBloqueoAsientoRemotoDto(asientoId, "CONFLICTO", "Bloqueado por otra sesión"));
                    continue;
                } else {
                    // idempotente: ya lo tiene esta sesión → marcar OK y refrescar TTL
                    lockService.lockSeat(externalEventoId, sessionId, asientoId);
                    resultadosParciales.add(new ResultadoBloqueoAsientoRemotoDto(asientoId, "OK", null));
                    yaOkPorMismaSesion.add(asientoId);
                    continue;
                }
            }

            // libre localmente → considerar para invocar a cátedra
            idsValidos.add(asientoId);
            posiciones.add(posOpt.get());
        }

        // 3) Si no hay nada para pedir a cátedra, devolver lo parcial
        List<ResultadoBloqueoAsientoRemotoDto> resultadosFinales = new ArrayList<>(resultadosParciales);
        if (posiciones.isEmpty()) {
            return new RespuestaBloqueoAsientosRemotaDto(externalEventoId, sessionId, resultadosFinales);
        }

        var body = new org.example.tf25.proxy.dto.catedra.PeticionBloqueoAsientosRemotaDto(eventoId, posiciones);

        try {
            var respuestaCatedra = catedraRestClient.post()
                    .uri("/api/endpoints/v1/bloquear-asientos")
                    .header("X-Session-Id", sessionId)
                    .body(body)
                    .retrieve()
                    .body(org.example.tf25.proxy.dto.catedra.RespuestaBloqueoAsientosRemotaDto.class);

            if (respuestaCatedra != null) {
                var lista = respuestaCatedra.asientos();
                boolean resultadoOk = respuestaCatedra.resultado();
                String descripcion = respuestaCatedra.descripcion();

                if (lista != null && !lista.isEmpty()) {
                    for (AsientoBloqueadoRemoto a : lista) {
                        String id = "r" + a.fila() + "c" + a.columna();
                        String estadoUpstreamRaw = a.estado();
                        String estadoUpstream = estadoUpstreamRaw != null ? estadoUpstreamRaw.trim() : null;
                        String estado = mapEstadoCatedraRobusto(estadoUpstream);
                        boolean esProblema = "CONFLICTO".equals(estado) || "INVALIDO".equals(estado) || "DESCONOCIDO".equals(estado);
                        String mensaje = (!resultadoOk && esProblema && descripcion != null && !descripcion.isBlank()) ? descripcion : null;
                        resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, estado, mensaje));
                        if ("OK".equals(estado)) {
                            lockService.lockSeat(externalEventoId, sessionId, id);
                        }
                    }
                } else {
                    // No hay lista de asientos; si hay descripcion, propagarla
                    for (String id : idsValidos) {
                        resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, "ERROR",
                                (descripcion != null && !descripcion.isBlank()) ? descripcion : "Sin respuesta de cátedra"));
                    }
                }
            } else {
                // sin respuesta: marcar ERROR para los enviados a cátedra
                for (String id : idsValidos) {
                    resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, "ERROR", "Sin respuesta de cátedra"));
                }
            }

            return new RespuestaBloqueoAsientosRemotaDto(externalEventoId, sessionId, resultadosFinales);
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            // Si es 401, refrescar token y reintentar una vez
            if (ex.getRawStatusCode() == 401) {
                log.info("Proxy: 401 en bloqueo; refrescando token y reintentando una vez...");
                try {
                    authService.refreshToken();
                    var respuestaCatedra = catedraRestClient.post()
                            .uri("/api/endpoints/v1/bloquear-asientos")
                            .header("X-Session-Id", sessionId)
                            .body(body)
                            .retrieve()
                            .body(org.example.tf25.proxy.dto.catedra.RespuestaBloqueoAsientosRemotaDto.class);

                    if (respuestaCatedra != null) {
                        var lista = respuestaCatedra.asientos();
                        boolean resultadoOk = respuestaCatedra.resultado();
                        String descripcion = respuestaCatedra.descripcion();

                        if (lista != null && !lista.isEmpty()) {
                            for (AsientoBloqueadoRemoto a : lista) {
                                String id = "r" + a.fila() + "c" + a.columna();
                                String estadoUpstreamRaw = a.estado();
                                String estadoUpstream = estadoUpstreamRaw != null ? estadoUpstreamRaw.trim() : null;
                                String estado = mapEstadoCatedraRobusto(estadoUpstream);
                                boolean esProblema = "CONFLICTO".equals(estado) || "INVALIDO".equals(estado) || "DESCONOCIDO".equals(estado);
                                String mensaje = (!resultadoOk && esProblema && descripcion != null && !descripcion.isBlank()) ? descripcion : null;
                                resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, estado, mensaje));
                                if ("OK".equals(estado)) {
                                    lockService.lockSeat(externalEventoId, sessionId, id);
                                }
                            }
                        } else {
                            for (String id : idsValidos) {
                                resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, "ERROR",
                                        (descripcion != null && !descripcion.isBlank()) ? descripcion : "Sin respuesta de cátedra"));
                            }
                        }
                    } else {
                        for (String id : idsValidos) {
                            resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, "ERROR", "Sin respuesta de cátedra"));
                        }
                    }
                    return new RespuestaBloqueoAsientosRemotaDto(externalEventoId, sessionId, resultadosFinales);
                } catch (org.springframework.web.client.RestClientResponseException ex2) {
                    log.warn("Proxy: reintento tras refresh falló con HTTP {}: {}", ex2.getRawStatusCode(), ex2.getResponseBodyAsString());
                } catch (org.springframework.web.client.RestClientException ex2) {
                    log.warn("Proxy: reintento tras refresh falló por comunicación", ex2);
                }
            }
            log.warn("Proxy: error HTTP {} al bloquear asientos en cátedra: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            for (String id : idsValidos) {
                resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, "ERROR", "Error HTTP " + ex.getRawStatusCode() + " en cátedra"));
            }
            return new RespuestaBloqueoAsientosRemotaDto(externalEventoId, sessionId, resultadosFinales);
        } catch (org.springframework.web.client.RestClientException ex) {
            log.warn("Proxy: error de comunicación con cátedra al bloquear asientos", ex);
            for (String id : idsValidos) {
                resultadosFinales.add(new ResultadoBloqueoAsientoRemotoDto(id, "ERROR", "Error de comunicación con cátedra"));
            }
            return new RespuestaBloqueoAsientosRemotaDto(externalEventoId, sessionId, resultadosFinales);
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private static String mapEstadoCatedraRobusto(String estadoUpstream) {
        String e = norm(estadoUpstream);

        // Confirmados por evidencia real
        if (e.contains("OCUP")) return "CONFLICTO";  // Ocupado
        if (e.contains("NOVAL")) return "INVALIDO";  // NoValido

        // Éxitos (cuando aparezcan)
        if (e.contains("BLOQ")) return "OK";         // Bloqueado
        if (e.contains("RESERV")) return "OK";       // Reservado
        if (e.equals("OK")) return "OK";

        return "DESCONOCIDO";
    }

    private String mapEstadoCatedra(String estadoUpstream) {
        return mapEstadoCatedraRobusto(estadoUpstream);
    }

    private java.util.Optional<AsientoPosicionRemota> parseAsientoId(String id) {
        if (id == null) return java.util.Optional.empty();
        Matcher m = ASIENTO_ID_PATTERN.matcher(id.trim().toLowerCase(Locale.ROOT));
        if (!m.matches()) return java.util.Optional.empty();
        int fila = Integer.parseInt(m.group(1));
        int columna = Integer.parseInt(m.group(2));
        return java.util.Optional.of(new AsientoPosicionRemota(fila, columna));
    }
}
