package com.backend.bff_spring.dto;

import java.util.List;
import java.util.UUID;

public class UsuarioDto {
    private UUID idAzure;
    private Long idRol;
    private List<Long> obraIds;

    private String username;
    private String password;
    private String nombreCompleto;

    public UUID getIdAzure() { return idAzure; }
    public void setIdAzure(UUID idAzure) { this.idAzure = idAzure; }
    public Long getIdRol() { return idRol; }
    public void setIdRol(Long idRol) { this.idRol = idRol; }
    public List<Long> getObraIds() { return obraIds; }
    public void setObraIds(List<Long> obraIds) { this.obraIds = obraIds; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
}
