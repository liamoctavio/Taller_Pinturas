package com.function.model;

public class Usuario {
  private String id_azure;
  private Long id_rol;
  private RolRef rol;
  private String username;
  private String nombre_completo;

  public Usuario() {}

  public String getId_azure() { return id_azure; }
  public void setId_azure(String id_azure) { this.id_azure = id_azure; }

  public Long getId_rol() { return id_rol; }
  public void setId_rol(Long id_rol) { this.id_rol = id_rol; }

  public RolRef getRol() { return rol; }
  public void setRol(RolRef rol) { this.rol = rol; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getNombre_completo() { return nombre_completo; }
  public void setNombre_completo(String nombre_completo) { this.nombre_completo = nombre_completo; }
}
