package com.function.model;

public class Obra {
  private Long id_obra;
  private TipoObra tipo;
  private String titulo;
  private String descripcion;
    // no incluimos imagen en getter/setter por defecto para evitar payloads grandes,
  // pero lo permitimos en creación/actualización por campo imagenBase64
  private String imagenBase64;

  public Obra() {}

  public Long getId_obra() { return id_obra; }
  public void setId_obra(Long id_obra) { this.id_obra = id_obra; }

  public TipoObra getTipo() { return tipo; }
  public void setTipo(TipoObra tipo) { this.tipo = tipo; }

  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }

  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

  public String getImagenBase64() { return imagenBase64; }
  public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }
}
