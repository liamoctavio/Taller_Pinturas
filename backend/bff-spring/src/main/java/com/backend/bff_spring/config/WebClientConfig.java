package com.backend.bff_spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Value("${functions.eventosBaseUrl}")
  private String eventosBase;

  @Value("${functions.obrasBaseUrl}")
  private String obrasBase;

  @Value("${functions.usuariosBaseUrl}")
  private String usuariosBase;

  @Value("${service.auth.token:}")
  private String serviceAuthToken;

  private WebClient.Builder baseBuilder(String base) {
    WebClient.Builder b = WebClient.builder().baseUrl(base);
    if (serviceAuthToken != null && !serviceAuthToken.isBlank()) {
      if (serviceAuthToken.startsWith("key:")) {
        b.defaultHeader("x-functions-key", serviceAuthToken.substring(4));
      } else {
        b.defaultHeader("Authorization", "Bearer " + serviceAuthToken);
      }
    }
    return b;
  }

  @Bean
  public WebClient obrasClient() {
    return baseBuilder(obrasBase).build();
  }

  @Bean
  public WebClient eventosClient() {
    return baseBuilder(eventosBase).build();
  }

  @Bean
  public WebClient usuariosClient() {
    return baseBuilder(usuariosBase).build();
  }

  @Bean
  public WebClient graphqlClient() {
    return baseBuilder(obrasBase).build();
  }
}

