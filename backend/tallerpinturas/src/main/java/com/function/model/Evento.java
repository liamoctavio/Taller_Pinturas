package com.function.model;

import java.math.BigDecimal;

public class Evento {
  private Long id_eventos;
  private TipoEvento tipo;
  private UsuarioRef usuario;
  private RolRef rol;
  private String titulo;
  private String descripcion;
  private String fechaInicio;
  private String fechaTermino;
  private BigDecimal precio;
  private String direccion;
  private String id_azure;

  public Evento() {}

  public String getId_azure() { return id_azure;}
  public void setId_azure(String id_azure) { this.id_azure = id_azure; }

  public Long getId_eventos() { return id_eventos; }
  public void setId_eventos(Long id_eventos) { this.id_eventos = id_eventos; }

  public TipoEvento getTipo() { return tipo; }
  public void setTipo(TipoEvento tipo) { this.tipo = tipo; }

  public UsuarioRef getUsuario() { return usuario; }
  public void setUsuario(UsuarioRef usuario) { this.usuario = usuario; }

  public RolRef getRol() { return rol; }
  public void setRol(RolRef rol) { this.rol = rol; }

  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }

  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

  public String getFechaInicio() { return fechaInicio; }
  public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

  public String getFechaTermino() { return fechaTermino; }
  public void setFechaTermino(String fechaTermino) { this.fechaTermino = fechaTermino; }

  public BigDecimal getPrecio() { return precio; }
  public void setPrecio(BigDecimal precio) { this.precio = precio; }

  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
}
