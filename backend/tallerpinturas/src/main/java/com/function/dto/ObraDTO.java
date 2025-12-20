package com.function.dto;

import java.util.UUID;

public class ObraDTO {

    private Long idTipo;
    private String titulo;
    private String descripcion;
    private String imagen;
    private UUID idAzure;

    public Long getIdTipo() {
        return idTipo;
    }

    public void setIdTipo(Long idTipo) {
        this.idTipo = idTipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public UUID getIdAzure() {
        return idAzure;
    }

    public void setIdAzure(UUID idAzure) {
        this.idAzure = idAzure;
    }
}
