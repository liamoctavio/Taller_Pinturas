package com.function.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class EventoDTO {
  private Long idTipoEvento;
  private UUID idAzure;
  private Long idRol;
  private String titulo;
  private String descripcion;
  private Instant fechaInicio;
  private Instant fechaTermino;
  private BigDecimal precio;
  private String direccion;

  public Long getIdTipoEvento() { return idTipoEvento; }
  public UUID getIdAzure() { return idAzure; }
  public Long getIdRol() { return idRol; }
  public String getTitulo() { return titulo; }
  public String getDescripcion() { return descripcion; }
  public Instant getFechaInicio() { return fechaInicio; }
  public Instant getFechaTermino() { return fechaTermino; }
  public BigDecimal getPrecio() { return precio; }
  public String getDireccion() { return direccion; }


  void setIdTipoEvento(Long idTipoEvento) { this.idTipoEvento = idTipoEvento; }
  void setIdAzure(UUID idAzure) { this.idAzure = idAzure; }
  void setIdRol(Long idRol) { this.idRol = idRol; }
  void setTitulo(String titulo) { this.titulo = titulo; }
  void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
  void setFechaTermino(Instant fechaTermino) { this.fechaTermino = fechaTermino; }
  void setPrecio(BigDecimal precio) { this.precio = precio; }
  void setDireccion(String direccion) { this.direccion = direccion; }
}
