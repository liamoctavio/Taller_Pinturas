package com.backend.bff_spring.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EventoDto {
    private Long idEvento;
    private Long idTipoEvento;
    private UUID idAzure;
    private Long idRol;
    
    private String titulo;
    private String descripcion;
    private OffsetDateTime fechaInicio;
    private OffsetDateTime fechaTermino;
    private BigDecimal precio;
    private String direccion;
    
    public Long getIdEvento() {
        return idEvento;
    }
    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }
    public Long getIdTipoEvento() {
        return idTipoEvento;
    }
    public void setIdTipoEvento(Long idTipoEvento) {
        this.idTipoEvento = idTipoEvento;
    }
    public UUID getIdAzure() {
        return idAzure;
    }
    public void setIdAzure(UUID idAzure) {
        this.idAzure = idAzure;
    }
    public Long getIdRol() {
        return idRol;
    }
    public void setIdRol(Long idRol) {
        this.idRol = idRol;
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
    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }
    public void setFechaInicio(OffsetDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    public OffsetDateTime getFechaTermino() {
        return fechaTermino;
    }
    public void setFechaTermino(OffsetDateTime fechaTermino) {
        this.fechaTermino = fechaTermino;
    }
    public BigDecimal getPrecio() {
        return precio;
    }
    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    
}

