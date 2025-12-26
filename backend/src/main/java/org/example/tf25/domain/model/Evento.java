package org.example.tf25.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String descripcion;

    private LocalDateTime fechaHora;

    private Integer cupo;

    private BigDecimal precio;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    private EventoEstado estado = EventoEstado.ACTIVO;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Venta> ventas = new ArrayList<>();

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getCupo() {
        return cupo;
    }
    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }
    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public EventoEstado getEstado() {
        return estado;
    }

    public void setEstado(EventoEstado estado) {
        this.estado = estado;
    }

    public List<Venta> getVentas() {
        return ventas;
    }
    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }
}
