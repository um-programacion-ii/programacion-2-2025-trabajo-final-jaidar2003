package org.example.tf25.proxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tf25.proxy.dto.AsientoRemotoDto;
import org.example.tf25.proxy.dto.catedra.CatedraRedisAsientoDto;
import org.example.tf25.proxy.dto.catedra.CatedraRedisEventoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AsientosProxyService {

    private static final Logger log = LoggerFactory.getLogger(AsientosProxyService.class);

    private final StringRedisTemplate catedraRedis;
    private final StringRedisTemplate localRedis;
    private final ObjectMapper objectMapper;

    public AsientosProxyService(@Qualifier("catedraRedisTemplate") StringRedisTemplate catedraRedis,
                                StringRedisTemplate localRedis,
                                ObjectMapper objectMapper) {
        this.catedraRedis = catedraRedis;
        this.localRedis = localRedis;
        this.objectMapper = objectMapper;
    }

    public List<AsientoRemotoDto> obtenerAsientos(String externalEventoId) {
        if (externalEventoId == null || externalEventoId.isBlank()) {
            return List.of();
        }
        List<AsientoRemotoDto> result = new ArrayList<>();
        Set<String> processedSeatIds = new HashSet<>();

        // 1) Intentar cargar desde Redis de la cátedra
        String key = "evento_" + externalEventoId;
        try {
            String json = catedraRedis.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                CatedraRedisEventoDto payload = objectMapper.readValue(json, CatedraRedisEventoDto.class);
                if (payload != null && payload.asientos() != null) {
                    for (CatedraRedisAsientoDto a : payload.asientos()) {
                        if (a == null || a.fila() == null || a.columna() == null || a.estado() == null) {
                            continue;
                        }
                        String estadoRemoto = a.estado();
                        String estadoLocal;
                        
                        // Normalizamos estados comunes de la cátedra
                        if (estadoRemoto == null) continue;
                        String e = estadoRemoto.trim().toUpperCase();
                        
                        if (e.contains("BLOQ") || e.contains("RESERV")) {
                            estadoLocal = "Bloqueado";
                        } else if (e.contains("VEND") || e.contains("OCUP")) {
                            estadoLocal = "Vendido";
                        } else if (e.contains("LIBR") || e.contains("DISP")) {
                            // Si está libre, no lo agregamos a la lista de ocupados
                            continue;
                        } else {
                            // Por las dudas, si es cualquier otra cosa que no sea LIBRE, lo marcamos como bloqueado
                            estadoLocal = "Bloqueado";
                        }
                        
                        String id = "r" + a.fila() + "c" + a.columna();
                        result.add(new AsientoRemotoDto(id, a.fila(), a.columna(), estadoLocal));
                        processedSeatIds.add(id);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Proxy: error consultando Redis cátedra para evento {}: {}", externalEventoId, ex.toString());
        }

        // 2) Mezclar con locks locales (Option A recomendada)
        try {
            Set<String> localLockKeys = localRedis.keys("lock:" + externalEventoId + ":*");
            if (localLockKeys != null) {
                for (String lockKey : localLockKeys) {
                    // lockKey es "lock:eventId:seatId"
                    String[] parts = lockKey.split(":");
                    if (parts.length == 3) {
                        String seatId = parts[2];
                        if (!processedSeatIds.contains(seatId)) {
                            // Si no estaba en cátedra, lo agregamos como Bloqueado
                            // Necesitamos fila/columna, que sacamos del seatId (formato rNcM)
                            parseAsientoId(seatId).ifPresent(pos -> {
                                result.add(new AsientoRemotoDto(seatId, pos.fila(), pos.columna(), "Bloqueado"));
                                processedSeatIds.add(seatId);
                            });
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Proxy: error consultando locks locales para evento {}: {}", externalEventoId, ex.toString());
        }

        return result;
    }

    private java.util.Optional<AsientoPosicion> parseAsientoId(String id) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("r(\\d+)c(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(id);
            if (m.matches()) {
                return java.util.Optional.of(new AsientoPosicion(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
            }
        } catch (Exception ignored) {}
        return java.util.Optional.empty();
    }

    private record AsientoPosicion(int fila, int columna) {}
}
