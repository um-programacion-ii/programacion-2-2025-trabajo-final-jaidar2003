package org.example.tf25.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(indexes = {
        @Index(name = "idx_venta_estado_retry", columnList = "estado, next_retry_at")
})
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true, fetch = FetchType.EAGER) // EAGER para evitar LazyInitializationException en este TP
    private Evento evento;

    // ====== Campos "clásicos" (los que ya tenías) ======
    private String compradorEmail;
    private Integer cantidad;
    private BigDecimal total;
    private LocalDateTime fechaHora;

    // ====== Campos TF25 (Issue #11) ======
    @Column(name = "external_evento_id")
    private String externalEventoId;

    @Column(name = "session_id")
    private String sessionId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "venta_asientos", joinColumns = @JoinColumn(name = "venta_id"))
    @Column(name = "asiento_id")
    private Set<String> asientosIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "venta_ocupantes", joinColumns = @JoinColumn(name = "venta_id"))
    @Column(name = "nombre")
    private List<String> nombresOcupantes = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    private VentaEstado estado = VentaEstado.PENDIENTE;

    @Column(name = "intentos_notificacion")
    private int intentosNotificacion;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ====== getters/setters ======
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public String getCompradorEmail() { return compradorEmail; }
    public void setCompradorEmail(String compradorEmail) { this.compradorEmail = compradorEmail; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getExternalEventoId() { return externalEventoId; }
    public void setExternalEventoId(String externalEventoId) { this.externalEventoId = externalEventoId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Set<String> getAsientosIds() { return asientosIds; }
    public void setAsientosIds(Set<String> asientosIds) { this.asientosIds = asientosIds; }

    public List<String> getNombresOcupantes() { return nombresOcupantes; }
    public void setNombresOcupantes(List<String> nombresOcupantes) { this.nombresOcupantes = nombresOcupantes; }

    public VentaEstado getEstado() { return estado; }
    public void setEstado(VentaEstado estado) { this.estado = estado; }

    public int getIntentosNotificacion() { return intentosNotificacion; }
    public void setIntentosNotificacion(int intentosNotificacion) { this.intentosNotificacion = intentosNotificacion; }

    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }

    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Long getEventoId() {
        return (evento != null) ? evento.getId() : null;
    }
}
