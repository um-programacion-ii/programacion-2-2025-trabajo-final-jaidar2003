package org.example.tf25.proxy.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventoRemotoDto {

    private Long id;
    private String nombre;
    private String descripcion;
    private LocalDateTime fechaHora;
    private Integer cupo;
    private BigDecimal precio;

    public EventoRemotoDto() {
    }

    public EventoRemotoDto(Long id, String nombre, String descripcion, LocalDateTime fechaHora, Integer cupo, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.cupo = cupo;
        this.precio = precio;
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
}
