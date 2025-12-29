package org.example.tf25.proxy.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public class EventoRemotoDto {

    private Long id;

    // La cátedra envía "titulo"; lo mapeamos a nuestro campo "nombre"
    @JsonProperty("titulo")
    private String nombre;

    // La cátedra envía "resumen" en el listado resumido
    @JsonProperty("resumen")
    private String resumen;

    private String descripcion;

    // La cátedra envía "fecha" con zona (ej: 2026-01-10T11:00:00Z); usamos Instant
    @JsonProperty("fecha")
    private Instant fechaHora;

    // No siempre lo envía la cátedra en los endpoints resumidos
    private Integer cupo;

    // La cátedra envía "precioEntrada"; lo mapeamos a nuestro campo "precio"
    @JsonProperty("precioEntrada")
    private BigDecimal precio;

    // La cátedra envía un objeto "eventoTipo" con nombre y descripcion
    @JsonProperty("eventoTipo")
    private EventoTipoRemotoDto eventoTipo;
    private Integer filaAsientos;
    private Integer columnAsientos;

    public EventoRemotoDto() {
    }

    public EventoRemotoDto(Long id, String nombre, String resumen, String descripcion, Instant fechaHora, Integer cupo, BigDecimal precio, EventoTipoRemotoDto eventoTipo) {
        this.id = id;
        this.nombre = nombre;
        this.resumen = resumen;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.cupo = cupo;
        this.precio = precio;
        this.eventoTipo = eventoTipo;
    }

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

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Instant fechaHora) {
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

    public EventoTipoRemotoDto getEventoTipo() {
        return eventoTipo;
    }

    public void setEventoTipo(EventoTipoRemotoDto eventoTipo) {
        this.eventoTipo = eventoTipo;
    }
    public Integer getFilaAsientos() { return filaAsientos; }
    public void setFilaAsientos(Integer filaAsientos) { this.filaAsientos = filaAsientos; }
    public Integer getColumnAsientos() { return columnAsientos; }
    public void setColumnAsientos(Integer columnAsientos) { this.columnAsientos = columnAsientos; }
}
