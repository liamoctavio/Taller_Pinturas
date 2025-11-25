package com.function.model;

public class Obra {
  private Long id_obra;
  private Long id_tipo_obra;
  private String titulo;
  private String descripcion;
  // no incluimos imagen en getter/setter por defecto para evitar payloads grandes,
  // pero lo permitimos en creación/actualización por campo imagenBase64
  private String imagenBase64;

  public Obra() {}

  public Long getId_obra() { return id_obra; }
  public void setId_obra(Long id_obra) { this.id_obra = id_obra; }

  public Long getId_tipo_obra() { return id_tipo_obra; }
  public void setId_tipo_obra(Long id_tipo_obra) { this.id_tipo_obra = id_tipo_obra; }

  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }

  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

  public String getImagenBase64() { return imagenBase64; }
  public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }
}
