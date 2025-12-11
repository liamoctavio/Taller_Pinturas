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
import java.util.List;

public final class JwtAuthService {

  private static final String JWKS_URL = System.getenv("AZURE_AD_B2C_JWKS_URL");
  private static final String ISSUER = System.getenv("AZURE_AD_B2C_ISSUER");
  
  private static final String API_AUDIENCE = System.getenv("API_APP_ID_URI");
  private static final DefaultJWTProcessor<SecurityContext> JWT_PROC;

  static {
    if (JWKS_URL == null || ISSUER == null || API_AUDIENCE == null) {
      System.err.println("ERROR: Faltan variables de entorno.");
      System.err.println("Requerido: AZURE_AD_B2C_JWKS_URL, AZURE_AD_B2C_ISSUER, API_APP_ID_URI");
      throw new IllegalStateException("Faltan variables de entorno para JWT");
    }

    try {
      System.out.println("🔄 Configurando JWT Service...");
      System.out.println("   -> JWKS URL: " + JWKS_URL);
      System.out.println("   -> ISSUER ESPERADO: " + ISSUER);

      String jwksJson = fetchJwks(JWKS_URL);

      JWKSet jwkSet = JWKSet.parse(jwksJson);
      JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

      DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
      JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
      processor.setJWSKeySelector(keySelector);

      processor.setJWTClaimsSetVerifier((claims, ctx) -> {
        
        String iss = claims.getIssuer();
        if (iss == null || !iss.contains(ISSUER.replace("/v2.0/", ""))) {
           // Objects.equals(ISSUER, iss)
           // System.out.println("Warning: Issuer mismatch. Recibido: " + iss);
        }

        List<String> aud = claims.getAudience();
        if (aud == null || !aud.contains(API_AUDIENCE)) {
          throw new BadJWTException("Audience inválida. Esperaba: " + API_AUDIENCE + " Recibió: " + aud);
        }

        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
          throw new BadJWTException("Token expirado");
        }
      });

      JWT_PROC = processor;
      System.out.println("JWT Service configurado correctamente.");

    } catch (Exception e) {
      System.err.println("Error inicializando JwtAuthService: " + e.getMessage());
      throw new RuntimeException("Error configurando JwtAuthService", e);
    }
  }

  private JwtAuthService() {}

  public static JWTClaimsSet validate(String authHeader) throws BadJOSEException, JOSEException, ParseException {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing Bearer token");
    }
    String token = authHeader.substring("Bearer ".length()).trim();
    return JWT_PROC.process(token, null);
  }

  private static String fetchJwks(String jwksUrl) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(jwksUrl)).timeout(Duration.ofSeconds(5)).GET().build();
    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
    
    if (resp.statusCode() != 200) {
      throw new IOException("Error fetching JWKS: HTTP " + resp.statusCode());
    }
    return resp.body();
  }
}