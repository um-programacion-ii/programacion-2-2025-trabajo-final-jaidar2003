package org.example.tf25.proxy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.Instant;

public class EventoProxyDto {

    private Long id;

    // Acepta tanto la forma de la cátedra (titulo) como la del proxy (nombre)
    @JsonAlias({"titulo", "nombre"})
    private String nombre;

    // Presente en la cátedra y propagado por el proxy
    @JsonAlias({"resumen"})
    private String resumen;

    private String descripcion;

    // Acepta "fecha" (cátedra) y "fechaHora" (proxy)
    @JsonAlias({"fecha", "fechaHora"})
    private Instant fechaHora;

    private Integer cupo;

    // Acepta "precioEntrada" (cátedra) y "precio" (proxy)
    @JsonAlias({"precioEntrada", "precio"})
    private BigDecimal precio;

    private Integer filaAsientos;
    private Integer columnAsientos;
    // Nombre igual en ambos, pero dejamos alias por seguridad
    @JsonAlias({"eventoTipo"})
    private EventoTipoProxyDto eventoTipo;

    public EventoProxyDto() {}

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

    public EventoTipoProxyDto getEventoTipo() {
        return eventoTipo;
    }

    public void setEventoTipo(EventoTipoProxyDto eventoTipo) {
        this.eventoTipo = eventoTipo;
    }
    public Integer getFilaAsientos() { return filaAsientos; }
    public void setFilaAsientos(Integer filaAsientos) { this.filaAsientos = filaAsientos; }
    public Integer getColumnAsientos() { return columnAsientos; }
    public void setColumnAsientos(Integer columnAsientos) { this.columnAsientos = columnAsientos; }
}
