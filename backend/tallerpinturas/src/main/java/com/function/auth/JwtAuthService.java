package com.function.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSetUnavailableException;
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
import java.util.Objects;

public final class JwtAuthService {

  private static final String TENANT_ID = System.getenv("AAD_TENANT_ID");
  private static final String API_AUDIENCE = System.getenv("API_APP_ID_URI"); // p.ej. api://<id> o client-id
  private static final String BFF_CLIENT_ID = System.getenv("BFF_CLIENT_ID"); // opcional para check azp/appid
  private static final String ISSUER;
  private static final DefaultJWTProcessor<SecurityContext> JWT_PROC;

  static {
    if (TENANT_ID == null || API_AUDIENCE == null) {
      throw new IllegalStateException("Faltan env vars AAD_TENANT_ID o API_APP_ID_URI");
    }
    ISSUER = "https://login.microsoftonline.com/" + TENANT_ID + "/v2.0";
    String jwksUrl = ISSUER + "/discovery/v2.0/keys";

    try {

      // fetch JWKS (puede lanzar IOException o InterruptedException)
      String jwksJson = fetchJwks(jwksUrl);

      // parse JWKSet (puede lanzar java.text.ParseException que capturamos abajo)
      JWKSet jwkSet = JWKSet.parse(jwksJson);

      // create immutable JWKSource from the fetched JWKSet
      JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

      // configure processor and key selector for RS256
      DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
      JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
      processor.setJWSKeySelector(keySelector);

      // set custom claims verifier (issuer, audience, expiration)
      processor.setJWTClaimsSetVerifier((claims, ctx) -> {
        // issuer
        String iss = claims.getIssuer();
        if (!Objects.equals(ISSUER, iss)) {
          throw new BadJWTException("issuer invalido");
        }
        // audience
        List<String> aud = claims.getAudience();
        if (aud == null || !aud.contains(API_AUDIENCE)) {
          throw new BadJWTException("audience invalida");
        }
        // expiration
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
          throw new BadJWTException("token expirado");
        }
      });

      JWT_PROC = processor;

    } catch (Exception e) {
      throw new JwtAuthException("Error configurando JwtAuthService: " + e.getMessage(), e);
    }
  }

  private JwtAuthService() {
  }

  /**
   * Valida la cabecera Authorization: "Bearer <token>" y devuelve JWTClaimsSet.
   *
   * Lanza BadJOSEException o JOSEException si el token es inválido o no puede ser
   * procesado.
   * 
   * @throws ParseException
   */
  public static JWTClaimsSet validate(String authHeader) throws BadJOSEException, JOSEException, ParseException {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing Bearer token");
    }
    String token = authHeader.substring("Bearer ".length()).trim();

    // process token (lanza BadJOSEException o JOSEException si inválido)
    JWTClaimsSet claims = JWT_PROC.process(token, null);

    // Optional: verificar que el token fue emitido para la app BFF (azp/appid)
    if (BFF_CLIENT_ID != null && !BFF_CLIENT_ID.isBlank()) {
      Object azp = claims.getClaim("azp");
      Object appid = claims.getClaim("appid");
      boolean ok = (azp != null && BFF_CLIENT_ID.equals(String.valueOf(azp)))
          || (appid != null && BFF_CLIENT_ID.equals(String.valueOf(appid)));
      if (!ok)
        throw new BadJWTException("token no emitido para la app BFF");
    }

    return claims;
  }

  // ----- Helpers -----

  /**
   * Hace un fetch síncrono del JWKS. Propaga IOException e InterruptedException
   * (si el hilo fue interrumpido).
   */
  private static String fetchJwks(String jwksUrl) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(jwksUrl))
        .timeout(Duration.ofSeconds(5))
        .GET()
        .build();

    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() / 100 != 2) {
      throw new IOException("Error fetching JWKS: HTTP " + resp.statusCode() + " - " + resp.body());
    }
    String body = resp.body();
    if (body == null || body.isBlank())
      throw new IOException("Empty JWKS response");
    return body;
  }
}
