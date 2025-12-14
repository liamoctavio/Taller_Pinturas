package com.backend.bff_spring.dto;

public class ObrasDto {
    private Long idObra;
    private Long idTipoObra;
    private String titulo;
    private String descripcion;
    private String imagen;

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
}
