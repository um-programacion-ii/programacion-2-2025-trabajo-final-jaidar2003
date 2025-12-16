package org.example.tf25.proxy.service.dto;

public class EventoTipoRemotoDto {

    private String nombre;
    private String descripcion;

    public EventoTipoRemotoDto() {
    }

    public EventoTipoRemotoDto(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
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
}
