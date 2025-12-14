package com.backend.bff_spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ObrasDto {
    private Long idObra;
    private Long idTipoObra;
    private String titulo;
    private String descripcion;
    @JsonProperty("imagenBase64")
    private String imagen;
    @JsonProperty("id_azure")
    private String idAzure;

    public Long getIdObra() { return idObra; }
    public void setIdObra(Long idObra) { this.idObra = idObra; }
    public Long getIdTipoObra() { return idTipoObra; }
    public void setIdTipoObra(Long idTipoObra) { this.idTipoObra = idTipoObra; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getimagen() { return imagen; }
    public void setimagen(String imagen) { this.imagen = imagen; }
    public String getIdAzure() { return idAzure; }
    public void setIdAzure(String idAzure) { this.idAzure = idAzure; }
}
