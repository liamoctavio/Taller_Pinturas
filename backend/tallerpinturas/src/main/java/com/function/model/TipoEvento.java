package com.function.model;

public class TipoEvento {
  private Long id_tipo_evento;
  private String nombre;
  public TipoEvento() {}
  public Long getId_tipo_evento() { return id_tipo_evento; }
  public void setId_tipo_evento(Long id_tipo_evento) { this.id_tipo_evento = id_tipo_evento; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
}
