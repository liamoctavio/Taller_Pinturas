package com.function.model;

public class Evento {
  private Long id_eventos;
  private Long id_tipo_evento;
  private String id_azure;
  private Long id_rol;
  private String titulo;
  private String descripcion;
  private String fechaInicio;
  private String fechaTermino;
  private Double precio;
  private String direccion;

  public Evento() {}

  // getters y setters
  public Long getId_eventos() { return id_eventos; }
  public void setId_eventos(Long id_eventos) { this.id_eventos = id_eventos; }
  public Long getId_tipo_evento() { return id_tipo_evento; }
  public void setId_tipo_evento(Long id_tipo_evento) { this.id_tipo_evento = id_tipo_evento; }
  public String getId_azure() { return id_azure; }
  public void setId_azure(String id_azure) { this.id_azure = id_azure; }
  public Long getId_rol() { return id_rol; }
  public void setId_rol(Long id_rol) { this.id_rol = id_rol; }
  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }
  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  public String getFechaInicio() { return fechaInicio; }
  public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
  public String getFechaTermino() { return fechaTermino; }
  public void setFechaTermino(String fechaTermino) { this.fechaTermino = fechaTermino; }
  public Double getPrecio() { return precio; }
  public void setPrecio(Double precio) { this.precio = precio; }
  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
}
