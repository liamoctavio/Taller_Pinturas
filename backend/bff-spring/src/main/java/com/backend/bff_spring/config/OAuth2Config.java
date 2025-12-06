package com.backend.bff_spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

// @Configuration
// public class OAuth2Config {

//   @Bean
//   public OAuth2AuthorizedClientManager authorizedClientManager(
//       ClientRegistrationRepository clientRegistrationRepository,
//       OAuth2AuthorizedClientRepository authorizedClientRepository) {

//     OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
//         .clientCredentials()
//         .build();

//     DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(
//         clientRegistrationRepository, authorizedClientRepository);
//     manager.setAuthorizedClientProvider(provider);
//     return manager;
//   }

//   @Bean
//   public WebClient functionsWebClient(OAuth2AuthorizedClientManager manager) {
//     ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
//         new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
//     oauth.setDefaultClientRegistrationId("bff-client");
//     return WebClient.builder()
//         .filter(oauth)
//         .build();
//   }
// }

// @Configuration
// public class OAuth2Config {

//     // Hemos eliminado el "AuthorizedClientManager" porque causaba conflicto 
//     // entre Servlet y WebFlux.

//     @Bean
//     public WebClient functionsWebClient() {
//         // Creamos un WebClient simple, sin filtros de seguridad OAuth2.
//         // Esto permitirá que el BFF arranque y pueda llamar a la API
//         // (siempre y cuando la API no exija token real, cosa que ya bypasseamos).
//         return WebClient.builder()
//                 .build();
//     }
// }

@Configuration
@EnableWebFluxSecurity // <-- Esto activa la configuración de seguridad para WebFlux
public class OAuth2Config {

    // ESTE ES EL GUARDIÁN DE LA PUERTA
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Desactivamos CSRF (protección de formularios) porque es una API
            .csrf(csrf -> csrf.disable()) 
            // Reglas de autorización
            .authorizeExchange(exchanges -> exchanges
                // .pathMatchers("/bff/**").permitAll() // Podríamos ser específicos
                .anyExchange().permitAll() // PERO MEJOR: Dejamos pasar TODO (Modo Dios)
            )
            .build();
    }

    // Mantenemos el cliente web que creamos antes
    @Bean
    public WebClient functionsWebClient() {
        return WebClient.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // <--- AUMENTAMOS EL LÍMITE A 10MB
            .build();
    }
}