package com.function.auth;

import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.proc.*;
import com.nimbusds.jwt.*;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;

import java.net.URL;
import java.util.List;
import java.util.Date;

public final class JwtAuthService {
  private static final String TENANT_ID = System.getenv("AAD_TENANT_ID");
  private static final String API_AUDIENCE = System.getenv("API_APP_ID_URI"); // p.ej. api://<id> o client-id
  private static final String BFF_CLIENT_ID = System.getenv("BFF_CLIENT_ID"); // opcional para check azp/appid
  private static final String ISSUER;
  private static final ConfigurableJWTProcessor<SecurityContext> JWT_PROC;

  static {
    if (TENANT_ID == null || API_AUDIENCE == null) throw new IllegalStateException("Faltan env vars AAD_TENANT_ID o API_APP_ID_URI");
    ISSUER = "https://login.microsoftonline.com/" + TENANT_ID + "/v2.0";
    try {
      String jwksUrl = ISSUER + "/discovery/v2.0/keys";
      ResourceRetriever resourceRetriever = new DefaultResourceRetriever(2000, 2000);
      JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUrl), resourceRetriever);
      JWT_PROC = new DefaultJWTProcessor<>();
      JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
      JWT_PROC.setJWSKeySelector(keySelector);

      JWT_PROC.setJWTClaimsSetVerifier((claims, context) -> {
        // issuer
        if (!ISSUER.equals(claims.getIssuer())) throw new BadJWTException("issuer invalido");
        // audience must include API_AUDIENCE (can be client id or AppId URI)
        List<String> aud = claims.getAudience();
        if (aud == null || !aud.contains(API_AUDIENCE)) throw new BadJWTException("audience invalida");
        // expiration
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) throw new BadJWTException("token expirado");
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private JwtAuthService() {}

  public static JWTClaimsSet validate(String authHeader) throws Exception {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new IllegalArgumentException("Missing Bearer token");
    String token = authHeader.substring("Bearer ".length()).trim();
    SecurityContext ctx = null;
    JWTClaimsSet claims = JWT_PROC.process(token, ctx);
    if (BFF_CLIENT_ID != null) {
      Object azp = claims.getClaim("azp");
      Object appid = claims.getClaim("appid");
      boolean ok = (azp != null && azp.toString().equals(BFF_CLIENT_ID)) || (appid != null && appid.toString().equals(BFF_CLIENT_ID));
      if (!ok) throw new BadJWTException("token no emitido para la app BFF");
    }
    return claims;
  }
}
