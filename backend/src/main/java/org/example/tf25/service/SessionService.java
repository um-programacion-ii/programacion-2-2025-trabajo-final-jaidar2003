package org.example.tf25.service;

import lombok.extern.slf4j.Slf4j;
import org.example.tf25.service.dto.PasoFlujoCompra;
import org.example.tf25.service.dto.SessionState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionService {

    // Simulación de almacenamiento en memoria al no usar Redis en el backend
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionState crearNuevaSesionParaEvento(String userId, String externalEventoId) {
        String sessionId = UUID.randomUUID().toString();
        SessionState state = new SessionState(
                sessionId,
                userId,
                externalEventoId,
                Set.of(), // sin asientos seleccionados al inicio
                PasoFlujoCompra.MAPA_ASIENTOS // o SELECCION_EVENTO según cómo prefieras
        );

        sessions.put(sessionId, state);
        log.info("Creada nueva sesión {} (en memoria) para evento {}", sessionId, externalEventoId);
        return state;
    }

    public Optional<SessionState> obtenerSesion(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public SessionState guardarSesion(SessionState state) {
        if (state.getSessionId() == null) {
            throw new IllegalArgumentException("SessionId no puede ser null al guardar sesión");
        }
        sessions.put(state.getSessionId(), state);
        return state;
    }

    public void eliminarSesion(String sessionId) {
        if (sessionId == null) return;
        sessions.remove(sessionId);
    }
}
