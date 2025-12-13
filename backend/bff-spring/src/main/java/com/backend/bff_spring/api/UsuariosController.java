package com.backend.bff_spring.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.bff_spring.dto.UsuarioDto;
import com.backend.bff_spring.dto.VincularObraDto;
import com.backend.bff_spring.util.HttpForwarder;
import com.nimbusds.oauth2.sdk.Response;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bff/usuarios")
@CrossOrigin(origins = "*")
public class UsuariosController {

  private final WebClient usuariosClient;

  private static final String USUARIOS_ID = "/api/usuarios/{id}";

  public UsuariosController(@Qualifier("usuariosClient") WebClient usuariosClient) {
    this.usuariosClient = usuariosClient;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> listar(@RequestHeader Map<String,String> headers) {
    return usuariosClient.get().uri("/api/usuarios")
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("[]")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> uno(@PathVariable UUID id, @RequestHeader Map<String,String> headers) {
    return usuariosClient.get().uri(USUARIOS_ID, id.toString())
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> crear(@RequestBody UsuarioDto dto, @RequestHeader Map<String,String> headers) {
    return usuariosClient.post().uri("/api/usuarios")
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> actualizar(@PathVariable UUID id, @RequestBody UsuarioDto dto, @RequestHeader Map<String,String> headers) {
    return usuariosClient.put().uri(USUARIOS_ID, id.toString())
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @DeleteMapping(value = "/{id}")
  public Mono<ResponseEntity<Void>> eliminar(@PathVariable UUID id, @RequestHeader Map<String,String> headers) {
    return usuariosClient.delete().uri(USUARIOS_ID, id.toString())
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.statusCode().value()).build()));
  }

  @GetMapping(value = "/{id}/obras", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> listarObras(@PathVariable String id, @RequestHeader Map<String,String> headers) {
    return usuariosClient.get().uri(USUARIOS_ID + "/obras", id)  // Genera: "/api/usuarios/{id}/obras"
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("[]")
              .map(b -> ResponseEntity.status(resp.statusCode())
                .contentType(MediaType.APPLICATION_JSON).body(b)));
  }

  @PostMapping(value = "/{id}/obras")
  public Mono<ResponseEntity<Void>> vincularObra(@PathVariable String id, @RequestBody VincularObraDto dto, @RequestHeader Map<String,String> headers) {
    return usuariosClient.post().uri(USUARIOS_ID + "/obras", id) // Genera: "/api/usuarios/{id}/obras"
        .contentType(MediaType.APPLICATION_JSON)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .bodyValue(dto)
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.statusCode()).build()));
  }

  @DeleteMapping(value = "/{id}/obras/{obraId}")
  public Mono<ResponseEntity<Void>> desvincularObra(@PathVariable String id, @PathVariable Long obraId, @RequestHeader Map<String,String> headers) {
    return usuariosClient.delete().uri(USUARIOS_ID + "/obras/{obraId}", id, obraId) // Genera: "/api/usuarios/{id}/obras/{obraId}"
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.statusCode()).build()));
  }

  // Sincronizar usuario (Login) ESTO ES PARA EL LOGIN 
    @PostMapping("/sync") 
    public Mono<Object> syncUsuario(@RequestBody Object usuarioJson) {
      return usuariosClient.post()
              .uri("/api/usuarios/sync") // Esta es la ruta hacia Azure (Backend), esa déjala igual
              .bodyValue(usuarioJson)
              .retrieve()
              .bodyToMono(Object.class);
  }
  
}
