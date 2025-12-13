package com.backend.bff_spring.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.bff_spring.dto.ObrasDto;
import com.backend.bff_spring.util.HttpForwarder;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/bff/obras")
@CrossOrigin(origins = "*")
public class ObrasController {

  private final WebClient obrasClient;

  public ObrasController(@Qualifier("obrasClient") WebClient obrasClient) {
    this.obrasClient = obrasClient;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> listar(@RequestHeader Map<String,String> headers,
                                             @RequestParam(name="includeImage", required=false) Boolean includeImage) {
    String uri = "/api/obras" + (Boolean.TRUE.equals(includeImage) ? "?includeImage=true" : "");
    return obrasClient.get().uri(uri)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("[]")
                .map(body -> ResponseEntity.status(resp.statusCode().value())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> uno(@PathVariable("id") Long id,
                                          @RequestHeader Map<String,String> headers,
                                          @RequestParam(name="includeImage", required=false) Boolean includeImage) {
    String uri = "/api/obras/" + id + (Boolean.TRUE.equals(includeImage) ? "?includeImage=true" : "");
    return obrasClient.get().uri(uri)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> crear(@RequestBody ObrasDto dto,
                                            @RequestHeader Map<String,String> headers) {
    return obrasClient.post().uri("/api/obras")
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> actualizar(@PathVariable Long id,
                                                 @RequestBody ObrasDto dto,
                                                 @RequestHeader Map<String,String> headers) {
    return obrasClient.put().uri("/api/obras/{id}", id)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.statusCode().value()).contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @DeleteMapping(value = "/{id}")
  public Mono<ResponseEntity<Void>> eliminar(@PathVariable Long id,
                                             @RequestHeader Map<String,String> headers) {
    return obrasClient.delete().uri("/api/obras/{id}", id)
        .headers(h -> HttpForwarder.copyAuthHeaders(h, headers))
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.statusCode().value()).build()));
  }
}
