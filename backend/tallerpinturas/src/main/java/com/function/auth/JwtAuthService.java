package com.function.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Duration;
import java.util.Date;

public final class JwtAuthService {

  private static volatile DefaultJWTProcessor<SecurityContext> JWT_PROC;

  private JwtAuthService() {}

  public static JWTClaimsSet validate(String authHeader)
      throws BadJOSEException, JOSEException, ParseException {

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing Bearer token");
    }

    ensureInitialized();
    String token = authHeader.substring("Bearer ".length()).trim();
    return JWT_PROC.process(token, null);
  }

  private static synchronized void ensureInitialized() {
    if (JWT_PROC != null) return;

    try {
      String jwksUrl = System.getenv("AZURE_AD_B2C_JWKS_URL");
      String issuer  = System.getenv("AZURE_AD_B2C_ISSUER");
      String audience = System.getenv("API_APP_ID_URI");

      if (jwksUrl == null || issuer == null || audience == null) {
        throw new IllegalStateException("Faltan variables de entorno JWT");
      }

      String jwksJson = fetchJwks(jwksUrl);

      JWKSet jwkSet = JWKSet.parse(jwksJson);
      JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

      DefaultJWTProcessor<SecurityContext> proc = new DefaultJWTProcessor<>();
      proc.setJWSKeySelector(
          new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)
      );

      proc.setJWTClaimsSetVerifier((claims, ctx) -> {

        if (!claims.getAudience().contains(audience)) {
          throw new BadJWTException("Audience inválida");
        }

        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
          throw new BadJWTException("Token expirado");
        }
      });

      JWT_PROC = proc;

    } catch (Exception e) {
      throw new RuntimeException("Error inicializando JwtAuthService", e);
    }
  }

  private static String fetchJwks(String jwksUrl)
      throws IOException, InterruptedException {

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(jwksUrl))
        .timeout(Duration.ofSeconds(5))
        .GET()
        .build();

    HttpResponse<String> resp =
        client.send(req, HttpResponse.BodyHandlers.ofString());

    if (resp.statusCode() != 200) {
      throw new IOException("Error fetching JWKS");
    }

    return resp.body();
  }
}
