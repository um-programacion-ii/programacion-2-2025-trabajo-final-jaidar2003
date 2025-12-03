package org.example.tf25.service;

import lombok.extern.slf4j.Slf4j;
import org.example.tf25.service.dto.PasoFlujoCompra;
import org.example.tf25.service.dto.SessionState;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "tf25:sessions:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, SessionState> sessionRedisTemplate;

    public SessionService(RedisTemplate<String, SessionState> sessionRedisTemplate) {
        this.sessionRedisTemplate = sessionRedisTemplate;
    }

    private String buildKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    public SessionState crearNuevaSesionParaEvento(String userId, String externalEventoId) {
        String sessionId = UUID.randomUUID().toString();
        SessionState state = new SessionState(
                sessionId,
                userId,
                externalEventoId,
                Set.of(), // sin asientos seleccionados al inicio
                PasoFlujoCompra.MAPA_ASIENTOS // o SELECCION_EVENTO según cómo prefieras
        );

        sessionRedisTemplate.opsForValue().set(buildKey(sessionId), state, SESSION_TTL);
        log.info("Creada nueva sesión {} para evento {}", sessionId, externalEventoId);
        return state;
    }

    public Optional<SessionState> obtenerSesion(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        SessionState state = sessionRedisTemplate.opsForValue().get(buildKey(sessionId));
        if (state != null) {
            // Renovamos TTL (sesión deslizante)
            sessionRedisTemplate.expire(buildKey(sessionId), SESSION_TTL);
        }
        return Optional.ofNullable(state);
    }

    public SessionState guardarSesion(SessionState state) {
        if (state.getSessionId() == null) {
            throw new IllegalArgumentException("SessionId no puede ser null al guardar sesión");
        }
        sessionRedisTemplate.opsForValue().set(buildKey(state.getSessionId()), state, SESSION_TTL);
        return state;
    }

    public void eliminarSesion(String sessionId) {
        if (sessionId == null) return;
        sessionRedisTemplate.delete(buildKey(sessionId));
    }
}
