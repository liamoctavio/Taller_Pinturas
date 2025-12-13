package com.backend.bff_spring.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/bff")
@CrossOrigin(origins = "*")
public class GraphQLProxyController {

  private final WebClient graphqlClient;

  public GraphQLProxyController(@Qualifier("graphqlClient") WebClient graphqlClient) {
    this.graphqlClient = graphqlClient;
  }

  @PostMapping(value = "/graphql", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> post(@RequestBody Map<String, Object> body,
                                           @RequestHeader Map<String,String> headers) {
    Object q = body.get("query");
    if (q == null || !StringUtils.hasText(q.toString())) {
      return Mono.just(ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"error\":\"Body JSON inválido. Esperado: { \\\"query\\\": \\\"...\\\" }\"}"));
    }

    return graphqlClient.post()
        .uri("/api/graphql")
        .headers(h -> {
          String auth = headers.getOrDefault("Authorization", headers.get("authorization"));
          if (auth != null && !auth.isBlank()) h.set("Authorization", auth);
        })
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("{}")
                .map(b -> ResponseEntity.status(resp.statusCode().value())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(b)));
  }

  @GetMapping(value = "/graphql", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> get(@RequestParam(name = "query", required = false) String query,
                                          @RequestParam(name = "variables", required = false) String variablesJson) {
    if (!StringUtils.hasText(query)) {
      return Mono.just(ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"error\":\"Falta query en querystring\"}"));
    }
    Map<String,Object> payload = Map.of("query", query, "variables", (variablesJson != null ? variablesJson : Map.of()));
    return graphqlClient.post()
        .uri("/api/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("{}")
                .map(b -> ResponseEntity.status(resp.statusCode().value())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(b)));
  }
}

