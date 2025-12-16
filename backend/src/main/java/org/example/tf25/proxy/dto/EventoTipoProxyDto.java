package org.example.tf25.proxy.dto;

public class EventoTipoProxyDto {

    private String nombre;
    private String descripcion;

    public EventoTipoProxyDto() {}

    public EventoTipoProxyDto(String nombre, String descripcion) {
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
