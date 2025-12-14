package com.backend.bff_spring.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.bff_spring.dto.EventoDto;
import com.backend.bff_spring.util.HttpForwarder;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/bff/eventos")
public class EventosController {

  private final WebClient eventosClient;

  private static final String EVENTOS_ID = "/api/eventos/{id}";

  public EventosController(@Qualifier("eventosClient") WebClient eventosClient) {
    this.eventosClient = eventosClient;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> listar(@RequestHeader Map<String,String> headers) {
    return eventosClient.get().uri("/api/eventos")
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("[]")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> uno(@PathVariable Long id, @RequestHeader Map<String,String> headers) {
    return eventosClient.get().uri(EVENTOS_ID, id)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> crear(@RequestBody EventoDto dto, @RequestHeader Map<String,String> headers) {
    return eventosClient.post().uri("/api/eventos")
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> actualizar(@PathVariable Long id, @RequestBody EventoDto dto, @RequestHeader Map<String,String> headers) {
    return eventosClient.put().uri(EVENTOS_ID, id)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @DeleteMapping(value = "/{id}")
  public Mono<ResponseEntity<Void>> eliminar(@PathVariable Long id, @RequestHeader Map<String,String> headers) {
    return eventosClient.delete().uri(EVENTOS_ID, id)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.statusCode().value()).build()));
  }
}
