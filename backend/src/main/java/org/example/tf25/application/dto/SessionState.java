package org.example.tf25.application.dto;

import java.io.Serializable;
import java.util.Set;

/**
 * Estado de la sesión de compra (antes en Redis, ahora en memoria/DB).
 */
public class SessionState implements Serializable {

    private String sessionId;
    private String userId; // opcional por ahora (puede ir null)
    private String externalEventoId;
    private Set<String> asientosSeleccionados;
    private PasoFlujoCompra pasoActual;
    // Indica si hubo al menos un BLOQUEADO real en el último intento de bloqueo
    private boolean tuvoBloqueosExitosos;

    public SessionState() {
        // necesario para deserialización
    }

    public SessionState(
            String sessionId,
            String userId,
            String externalEventoId,
            Set<String> asientosSeleccionados,
            PasoFlujoCompra pasoActual
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.externalEventoId = externalEventoId;
        this.asientosSeleccionados = asientosSeleccionados;
        this.pasoActual = pasoActual;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getExternalEventoId() {
        return externalEventoId;
    }

    public Set<String> getAsientosSeleccionados() {
        return asientosSeleccionados;
    }

    public PasoFlujoCompra getPasoActual() {
        return pasoActual;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setExternalEventoId(String externalEventoId) {
        this.externalEventoId = externalEventoId;
    }

    public void setAsientosSeleccionados(Set<String> asientosSeleccionados) {
        this.asientosSeleccionados = asientosSeleccionados;
    }

    public void setPasoActual(PasoFlujoCompra pasoActual) {
        this.pasoActual = pasoActual;
    }

    public boolean isTuvoBloqueosExitosos() {
        return tuvoBloqueosExitosos;
    }

    public void setTuvoBloqueosExitosos(boolean tuvoBloqueosExitosos) {
        this.tuvoBloqueosExitosos = tuvoBloqueosExitosos;
    }
}
