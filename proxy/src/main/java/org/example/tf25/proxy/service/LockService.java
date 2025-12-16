package org.example.tf25.proxy.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Servicio simple de locks en Redis para bloquear asientos por 5 minutos (TTL).
 * Claves:
 *  - lock:{eventId}:{seatId} -> sessionId (TTL 5m)
 *  - session:{sessionId}:event:{eventId} -> Set de seatIds (TTL 5m)
 */
@Service
public class LockService {
    private final StringRedisTemplate redis;
    private static final Duration TTL = Duration.ofMinutes(5);

    public LockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String lockKey(String eventId, String seatId) {
        return "lock:" + eventId + ":" + seatId;
    }

    private String sessionSetKey(String sessionId, String eventId) {
        return "session:" + sessionId + ":event:" + eventId;
    }

    /**
     * Devuelve el sessionId que posee el lock del asiento, si existe.
     */
    public Optional<String> whoLocks(String eventId, String seatId) {
        String v = redis.opsForValue().get(lockKey(eventId, seatId));
        return Optional.ofNullable(v);
    }

    /**
     * Coloca/renueva el lock de un asiento para la sesión dada con TTL.
     */
    public void lockSeat(String eventId, String sessionId, String seatId) {
        String key = lockKey(eventId, seatId);
        redis.opsForValue().set(key, sessionId, TTL);
        // track en el set de la sesión
        String setKey = sessionSetKey(sessionId, eventId);
        redis.opsForSet().add(setKey, seatId);
        redis.expire(setKey, TTL);
    }

    /**
     * Coloca/renueva locks para múltiples asientos.
     */
    public void lockSeats(String eventId, String sessionId, List<String> seatIds) {
        for (String s : seatIds) {
            lockSeat(eventId, sessionId, s);
        }
    }
}
