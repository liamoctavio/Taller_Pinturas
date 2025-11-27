package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.sql.*;
import java.util.*;


/**
 * Azure Function HTTP para CRUD de obras.
 * Rutas:
 *  GET  /api/obras                -> listar (sin imagen)
 *  GET  /api/obras/{id}?includeImage=true -> obtener con imagen en base64
 *  POST /api/obras                -> crear (body incluye imagenBase64 opcional)
 *  PUT  /api/obras/{id}           -> actualizar (body incluye imagenBase64 opcional)
 *  DELETE /api/obras/{id}         -> eliminar (solo admin según header X-User-Roles)
 *
 * Requiere JwtAuthService.validate(authHeader) para validar token de servicio.
 */
public class ObrasFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("obras")
  public HttpResponseMessage handle(
      @HttpTrigger(name = "req",
                   methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                   authLevel = AuthorizationLevel.FUNCTION,
                   route = "obras/{id?}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idPath,
      final ExecutionContext ctx) {

    try {
      String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
      JWTClaimsSet svcClaims;
      try {
        svcClaims = com.function.auth.JwtAuthService.validate(authHeader);
      } catch (IllegalArgumentException iae) {
        return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Missing or malformed Authorization header").build();
      } catch (Exception e) {
        ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
        return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Invalid service token").build();
      }

      String method = request.getHttpMethod().name();
      switch (method) {
        case "GET":
          String includeImageParam = request.getQueryParameters().getOrDefault("includeImage", "false");
          boolean includeImage = "true".equalsIgnoreCase(includeImageParam) || "1".equals(includeImageParam);
          if (idPath == null) return listar(request, includeImage);
          return obtener(request, Long.parseLong(idPath), includeImage);
        case "POST":
          return crear(request);
        case "PUT":
          if (idPath == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id requerido en ruta").build();
          return actualizar(request, Long.parseLong(idPath));
        case "DELETE":
          if (idPath == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id requerido en ruta").build();
          // autorización: solo admin puede eliminar (según header del BFF)
          String rolesCsv = request.getHeaders().getOrDefault("x-user-roles", request.getHeaders().get("X-User-Roles"));
          boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
          if (!isAdmin) return request.createResponseBuilder(HttpStatus.FORBIDDEN).body("Operación reservada a admin").build();
          return eliminar(request, Long.parseLong(idPath));
        default:
          return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
      }
    } catch (Exception e) {
      ctx.getLogger().severe("Error general: " + e.getMessage());
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage())).build();
    }
  }

  private HttpResponseMessage listar(HttpRequestMessage<?> req, boolean includeImage) throws Exception {
    String sql = "SELECT o.id_obra, o.id_tipo_obra, t.nombre AS tipo_nombre, o.titulo, o.descripcion" +
                 (includeImage ? ", o.imagen" : "") +
                 " FROM obras o LEFT JOIN tipobra t ON o.id_tipo_obra = t.id_tipo_obra ORDER BY o.id_obra";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      List<Map<String,Object>> out = new ArrayList<>();
      while (rs.next()) {
        Map<String,Object> m = new HashMap<>();
        m.put("id_obra", rs.getLong("id_obra"));
        m.put("id_tipo_obra", rs.getLong("id_tipo_obra"));
        m.put("tipo_nombre", rs.getString("tipo_nombre"));
        m.put("titulo", rs.getString("titulo"));
        m.put("descripcion", rs.getString("descripcion"));
        if (includeImage) {
          byte[] b = rs.getBytes("imagen");
          if (b != null) m.put("imagenBase64", Base64.getEncoder().encodeToString(b));
          else m.put("imagenBase64", null);
        }
        out.add(m);
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id, boolean includeImage) throws Exception {
    String sql = "SELECT o.id_obra, o.id_tipo_obra, t.nombre AS tipo_nombre, o.titulo, o.descripcion" +
                 (includeImage ? ", o.imagen" : "") +
                 " FROM obras o LEFT JOIN tipobra t ON o.id_tipo_obra = t.id_tipo_obra WHERE o.id_obra = ?";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
        Map<String,Object> m = new HashMap<>();
        m.put("id_obra", rs.getLong("id_obra"));
        m.put("id_tipo_obra", rs.getLong("id_tipo_obra"));
        m.put("tipo_nombre", rs.getString("tipo_nombre"));
        m.put("titulo", rs.getString("titulo"));
        m.put("descripcion", rs.getString("descripcion"));
        if (includeImage) {
          byte[] b = rs.getBytes("imagen");
          if (b != null) m.put("imagenBase64", Base64.getEncoder().encodeToString(b));
          else m.put("imagenBase64", null);
        }
        return json(req, m, HttpStatus.OK);
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);
    Long idTipo = in.get("id_tipo_obra") != null ? ((Number)in.get("id_tipo_obra")).longValue() : null;
    String titulo = (String) in.get("titulo");
    String descripcion = (String) in.get("descripcion");
    String imagenBase64 = (String) in.get("imagenBase64");
    byte[] imageBytes = (imagenBase64 != null && !imagenBase64.isBlank()) ? Base64.getDecoder().decode(imagenBase64) : null;

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("INSERT INTO obras (id_tipo_obra, titulo, descripcion, imagen) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
      if (idTipo != null) ps.setLong(1, idTipo); else ps.setNull(1, Types.BIGINT);
      ps.setString(2, titulo);
      ps.setString(3, descripcion);
      if (imageBytes != null) ps.setBytes(4, imageBytes); else ps.setNull(4, Types.BINARY);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          long newId = keys.getLong(1);
          return obtener(req, newId, false);
        }
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);
    Long idTipo = in.get("id_tipo_obra") != null ? ((Number)in.get("id_tipo_obra")).longValue() : null;
    String titulo = (String) in.get("titulo");
    String descripcion = (String) in.get("descripcion");
    String imagenBase64 = (String) in.get("imagenBase64");
    byte[] imageBytes = (imagenBase64 != null && !imagenBase64.isBlank()) ? Base64.getDecoder().decode(imagenBase64) : null;

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "UPDATE obras SET id_tipo_obra = ?, titulo = ?, descripcion = ?, imagen = COALESCE(?, imagen) WHERE id_obra = ?")) {
      if (idTipo != null) ps.setLong(1, idTipo); else ps.setNull(1, Types.BIGINT);
      ps.setString(2, titulo);
      ps.setString(3, descripcion);
      if (imageBytes != null) ps.setBytes(4, imageBytes); else ps.setNull(4, Types.BINARY);
      ps.setLong(5, id);
      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id, false);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id) throws Exception {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM obras WHERE id_obra = ?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();
      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    }
  }

  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status) throws IOException {
    return req.createResponseBuilder(status)
        .header("Content-Type", "application/json")
        .body(MAPPER.writeValueAsString(body))
        .build();
  }

  private static String firstNonNullHeader(HttpRequestMessage<?> req, String... names) {
    for (String n : names) {
      String v = req.getHeaders().get(n);
      if (v != null) return v;
    }
    return null;
  }
}