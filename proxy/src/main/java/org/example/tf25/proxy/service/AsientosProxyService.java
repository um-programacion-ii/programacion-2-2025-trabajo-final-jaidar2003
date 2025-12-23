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
import java.util.List;

@Service
public class AsientosProxyService {

    private static final Logger log = LoggerFactory.getLogger(AsientosProxyService.class);

    private final StringRedisTemplate catedraRedis;
    private final ObjectMapper objectMapper;

    public AsientosProxyService(@Qualifier("catedraRedisTemplate") StringRedisTemplate catedraRedis,
                                ObjectMapper objectMapper) {
        this.catedraRedis = catedraRedis;
        this.objectMapper = objectMapper;
    }

    public List<AsientoRemotoDto> obtenerAsientos(String externalEventoId) {
        String key = "evento_" + externalEventoId;
        try {
            String json = catedraRedis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                log.debug("Proxy: clave Redis {} no encontrada en cátedra", key);
                return List.of();
            }

            CatedraRedisEventoDto payload = objectMapper.readValue(json, CatedraRedisEventoDto.class);
            if (payload == null || payload.asientos() == null) {
                return List.of();
            }

            List<AsientoRemotoDto> result = new ArrayList<>();
            for (CatedraRedisAsientoDto a : payload.asientos()) {
                if (a == null || a.fila() == null || a.columna() == null || a.estado() == null) {
                    continue;
                }
                String estadoRemoto = a.estado();
                String estadoLocal;
                if ("Bloqueado".equalsIgnoreCase(estadoRemoto)) {
                    estadoLocal = "Bloqueado";
                } else if ("Vendido".equalsIgnoreCase(estadoRemoto)) {
                    estadoLocal = "Vendido";
                } else {
                    // Otros estados no se publican; se consideran libres
                    continue;
                }
                String id = "r" + a.fila() + "c" + a.columna();
                result.add(new AsientoRemotoDto(id, a.fila(), a.columna(), estadoLocal));
            }
            return result;
        } catch (Exception ex) {
            // No romper el flujo; si hay error leyendo/parsing Redis, devolver vacío
            log.debug("Proxy: error consultando Redis cátedra para evento {}: {}", externalEventoId, ex.toString());
            return List.of();
        }
    }
}
