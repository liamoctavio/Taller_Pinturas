package com.function.model;

public class TipoObra {
  private Long id_tipo_obra;
  private String nombre;

  public TipoObra() {}

  public TipoObra(Long id_tipo_obra, String nombre) {
    this.id_tipo_obra = id_tipo_obra;
    this.nombre = nombre;
  }

  public Long getId_tipo_obra() { return id_tipo_obra; }
  public void setId_tipo_obra(Long id_tipo_obra) { this.id_tipo_obra = id_tipo_obra; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
}
