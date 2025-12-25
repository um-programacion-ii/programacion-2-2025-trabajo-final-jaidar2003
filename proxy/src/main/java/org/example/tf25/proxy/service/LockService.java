package org.example.tf25.proxy.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
     * Coloca el lock de un asiento para la sesión dada con TTL (SET NX).
     * @return true si lo pudo bloquear, false si ya estaba bloqueado por otro.
     */
    public boolean lockSeat(String eventId, String sessionId, String seatId) {
        String key = lockKey(eventId, seatId);
        Boolean ok = redis.opsForValue().setIfAbsent(key, sessionId, TTL);
        if (Boolean.TRUE.equals(ok)) {
            String setKey = sessionSetKey(sessionId, eventId);
            redis.opsForSet().add(setKey, seatId);
            redis.expire(setKey, TTL);
            return true;
        }
        // Si ya existe, verificamos si es de la misma sesión para renovar (idempotencia)
        String currentOwner = redis.opsForValue().get(key);
        if (sessionId.equals(currentOwner)) {
            redis.expire(key, TTL);
            String setKey = sessionSetKey(sessionId, eventId);
            redis.expire(setKey, TTL);
            return true;
        }
        return false;
    }

    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    public boolean unlockSeatIfOwner(String eventId, String sessionId, String seatId) {
        String key = lockKey(eventId, seatId);
        org.springframework.data.redis.core.script.DefaultRedisScript<Long> script =
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(UNLOCK_LUA, Long.class);
        Long r = redis.execute(script, java.util.List.of(key), sessionId);
        return r != null && r > 0;
    }

    /**
     * Coloca/renueva locks para múltiples asientos.
     */
    public void lockSeats(String eventId, String sessionId, List<String> seatIds) {
        for (String s : seatIds) {
            lockSeat(eventId, sessionId, s);
        }
    }

    public void releaseLocks(String eventId, String sessionId) {
        if (eventId == null || sessionId == null) return;
        String setKey = sessionSetKey(sessionId, eventId);
        Set<String> seatIds = redis.opsForSet().members(setKey);
        if (seatIds != null) {
            for (String s : seatIds) {
                unlockSeatIfOwner(eventId, sessionId, s);
            }
        }
        redis.delete(setKey);
    }

    public Long getRemainingTtl(String eventId, String seatId) {
        return redis.getExpire(lockKey(eventId, seatId), TimeUnit.SECONDS);
    }
}
