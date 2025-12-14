package com.backend.bff_spring.util;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import java.util.Map;

public final class HttpForwarder {
  private HttpForwarder() {}

  public static void copyAuthHeaders(HttpHeaders dest, Map<String,String> incoming) {
    if (incoming == null) return;
    String auth = incoming.getOrDefault("Authorization", incoming.get("authorization"));
    if (StringUtils.hasText(auth)) dest.set("Authorization", auth);

    String xf = incoming.getOrDefault("x-functions-key", incoming.get("X-Functions-Key"));
    if (StringUtils.hasText(xf)) dest.set("x-functions-key", xf);

    String roles = incoming.getOrDefault("x-user-roles", incoming.get("X-User-Roles"));
    if (StringUtils.hasText(roles)) dest.set("x-user-roles", roles);

    String ct = incoming.getOrDefault("Content-Type", incoming.get("content-type"));
    if (StringUtils.hasText(ct)) dest.set(HttpHeaders.CONTENT_TYPE, ct);
    else dest.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
  }
}
