package com.backend.bff_spring.dto;

public class VincularObraDto {
  private Long id_obra;
  private Boolean es_principal;

  public VincularObraDto() {}

  public Long getId_obra() { return id_obra; }
  public void setId_obra(Long id_obra) { this.id_obra = id_obra; }
  public Boolean getEs_principal() { return es_principal; }
  public void setEs_principal(Boolean es_principal) { this.es_principal = es_principal; }
}
