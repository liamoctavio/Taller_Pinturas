package com.function;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.function.db.Db;
import com.function.events.EventBusEG;
import com.function.model.Obra;
import com.function.model.TipoObra;
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
 * Nota: acepta input flexible en POST/PUT:
 *  - { "id_tipo_obra": 1, "titulo":"...", "descripcion":"...", "imagenBase64":"..." }
 *  - o { "tipo": { "id_tipo_obra": 1 }, "titulo":"...", ... }
 * 
 * Requiere JwtAuthService.validate(authHeader) para validar token de servicio.
 */
public class ObrasFunction {

  private static final ObjectMapper MAPPER = JsonMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .build();

  @FunctionName("obrasRoot")
  public HttpResponseMessage obrasRoot(
      @HttpTrigger(name = "req",
          methods = {HttpMethod.GET, HttpMethod.POST},
          authLevel = AuthorizationLevel.ANONYMOUS,
          route = "obras")
      HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws Exception {

    // validar token de servicio
    String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
    try {
      JWTClaimsSet claims = com.function.auth.JwtAuthService.validate(authHeader);
      // opcional: usar claims si necesario
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header("Content-Type","application/json")
          .body("{\"error\":\"Missing or malformed Authorization header\"}")
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header("Content-Type","application/json")
          .body("{\"error\":\"Invalid service token\"}")
          .build();
    }

    switch (request.getHttpMethod()) {
      case GET:  return listar(request);
      case POST: return crear(request, ctx);
      default:   return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  @FunctionName("obrasById")
  public HttpResponseMessage obrasById(
      @HttpTrigger(name = "req",
          methods = {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
          authLevel = AuthorizationLevel.ANONYMOUS,
          route = "obras/{id}")
      HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idStr,
      final ExecutionContext ctx) throws Exception {

    // validar token de servicio
    String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
    try {
      com.function.auth.JwtAuthService.validate(authHeader);
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header("Content-Type","application/json")
          .body("{\"error\":\"Missing or malformed Authorization header\"}")
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header("Content-Type","application/json")
          .body("{\"error\":\"Invalid service token\"}")
          .build();
    }

    long id;
    try { id = Long.parseLong(idStr); }
    catch (NumberFormatException e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("{\"error\":\"id inválido\"}")
          .header("Content-Type","application/json").build();
    }

    switch (request.getHttpMethod()) {
      case GET:    return obtener(request, id);
      case PUT:    return actualizar(request, id, ctx);
      case DELETE: return eliminar(request, id, request);
      default:     return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  // LISTAR (sin imagen por defecto)
  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws SQLException, IOException {
    String sql = "SELECT o.id_obra, o.id_tipo_obra, t.nombre AS tipo_nombre, o.titulo, o.descripcion " +
                 "FROM obras o LEFT JOIN tipobra t ON o.id_tipo_obra = t.id_tipo_obra ORDER BY o.id_obra";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      List<Obra> out = new ArrayList<>();
      while (rs.next()) {
        out.add(map(rs, false));
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  // OBTENER por id (incluye imagen si se solicita con includeImage)
  private HttpResponseMessage obtener(HttpRequestMessage<?> req, long id) throws SQLException, IOException {
    String includeImageParam = req.getQueryParameters().getOrDefault("includeImage", "false");
    boolean includeImage = "true".equalsIgnoreCase(includeImageParam) || "1".equals(includeImageParam);

    String sql = "SELECT o.id_obra, o.id_tipo_obra, t.nombre AS tipo_nombre, o.titulo, o.descripcion" +
                 (includeImage ? ", o.imagen" : "") +
                 " FROM obras o LEFT JOIN tipobra t ON o.id_tipo_obra = t.id_tipo_obra WHERE o.id_obra = ?";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Obra obra = map(rs, includeImage);
          return json(req, obra, HttpStatus.OK);
        }
        return req.createResponseBuilder(HttpStatus.NOT_FOUND)
            .header("Content-Type","application/json")
            .body("{\"error\":\"No encontrado\"}")
            .build();
      }
    }
  }

  // CREAR - acepta input flexible
  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req, ExecutionContext ctx) {
    try {
      String body = req.getBody().orElse("");
      if (body.isBlank()) {
        return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header("Content-Type","application/json")
            .body("{\"error\":\"Body vacío\"}")
            .build();
      }

      // parse raw into Map first to allow flexible input (id_tipo_obra or tipo:{id_tipo_obra})
      Map<String,Object> inMap = MAPPER.readValue(body, Map.class);

      Long idTipo = extractIdTipoFromMap(inMap);
      String titulo = asString(inMap.get("titulo"));
      String descripcion = asString(inMap.get("descripcion"));
      String imagenBase64 = asString(inMap.get("imagenBase64"));
      byte[] imageBytes = (imagenBase64 != null && !imagenBase64.isBlank()) ? Base64.getDecoder().decode(imagenBase64) : null;

      if (titulo == null || titulo.isBlank()) {
        return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header("Content-Type","application/json")
            .body("{\"error\":\"titulo es obligatorio\"}")
            .build();
      }

      try (Connection con = Db.connect();
           PreparedStatement ps = con.prepareStatement(
               "INSERT INTO obras (id_tipo_obra, titulo, descripcion, imagen) VALUES (?,?,?,?)",
               Statement.RETURN_GENERATED_KEYS)) {

        if (idTipo != null) ps.setLong(1, idTipo); else ps.setNull(1, Types.BIGINT);
        ps.setString(2, titulo);
        ps.setString(3, descripcion);
        if (imageBytes != null) ps.setBytes(4, imageBytes); else ps.setNull(4, Types.BINARY);

        int rows = ps.executeUpdate();
        if (rows > 0) {
          try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
              long newId = keys.getLong(1);
              try {
                Map<String,Object> data = new HashMap<>();
                data.put("id_obra", newId);
                data.put("titulo", titulo);
                EventBusEG.publish("Arte.Obra.Creada", "/obras/" + newId, data);
              } catch (Throwable t) {
                ctx.getLogger().info("EventBus publish failed (ignored): " + t.getMessage());
              }
              return obtener(req, newId);
            }
          }
        }
        return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type","application/json")
            .body("{\"error\":\"Insert no afectó filas\"}")
            .build();
      }
    } catch (com.fasterxml.jackson.databind.JsonMappingException jm) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("Content-Type","application/json")
          .body("{\"error\":\"JSON inválido\",\"detalle\":\"" +
                jm.getOriginalMessage().replace("\"","'") + "\"}")
          .build();
    } catch (SQLException ex) {
      return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("Content-Type","application/json")
          .body("{\"error\":\"DB\",\"sqlstate\":\"" + ex.getSQLState() +
                "\",\"code\":" + ex.getErrorCode() +
                ",\"message\":\"" + ex.getMessage().replace("\"","'") + "\"}")
          .build();
    } catch (Exception e) {
      return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("Content-Type","application/json")
          .body("{\"error\":\"server\",\"message\":\"" + e.getMessage().replace("\"","'") + "\"}")
          .build();
    }
  }

  // ACTUALIZAR - acepta input flexible similar a crear
  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, long id, ExecutionContext ctx) throws Exception {
    String body = req.getBody().orElse("{}");
    Map<String,Object> inMap = MAPPER.readValue(body, Map.class);

    Long idTipo = extractIdTipoFromMap(inMap);
    String titulo = asString(inMap.get("titulo"));
    String descripcion = asString(inMap.get("descripcion"));
    String imagenBase64 = asString(inMap.get("imagenBase64"));
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

      // publicar evento (opcional)
      try {
        Map<String,Object> data = new HashMap<>();
        data.put("id_obra", id);
        data.put("titulo", titulo);
        EventBusEG.publish("Arte.Obra.Actualizada", "/obras/" + id, data);
      } catch (Throwable t) {
        ctx.getLogger().info("EventBus publish failed (ignored): " + t.getMessage());
      }

      return obtener(req, id);
    }
  }

  // ELIMINAR
  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, long id, HttpRequestMessage<?> originalReq) throws SQLException {
    // autorización: solo admin (según header X-User-Roles)
    String rolesCsv = originalReq.getHeaders().getOrDefault("x-user-roles", originalReq.getHeaders().get("X-User-Roles"));
    boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
    if (!isAdmin) return originalReq.createResponseBuilder(HttpStatus.FORBIDDEN).body("{\"error\":\"Solo admin puede borrar\"}").build();

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM obras WHERE id_obra = ?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();
      if (rows > 0) {
        EventBusEG.publish("Arte.Obra.Eliminada", "/obras/" + id, Map.of("id_obra", id));
        return originalReq.createResponseBuilder(HttpStatus.NO_CONTENT).build();
      } else {
        return originalReq.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      }
    }
  }

  // Helper: map ResultSet -> Obra (con TipoObra anidado)
  private static Obra map(ResultSet rs, boolean includeImage) throws SQLException {
    Obra o = new Obra();

    long id = rs.getLong("id_obra");
    if (!rs.wasNull()) o.setId_obra(id);

    Long tid = null;
    long tmpTid = rs.getLong("id_tipo_obra");
    if (!rs.wasNull()) tid = tmpTid;

    String tipoNombre = rs.getString("tipo_nombre"); // may be null

    if (tid != null || tipoNombre != null) {
      TipoObra t = new TipoObra();
      t.setId_tipo_obra(tid);
      t.setNombre(tipoNombre);
      o.setTipo(t);
    } else {
      o.setTipo(null);
    }

    o.setTitulo(rs.getString("titulo"));
    o.setDescripcion(rs.getString("descripcion"));

    if (includeImage) {
      byte[] b = rs.getBytes("imagen");
      if (b != null) o.setImagenBase64(Base64.getEncoder().encodeToString(b));
      else o.setImagenBase64(null);
    }

    return o;
  }

  // util: extract id_tipo_obra from an input map which may have id_tipo_obra or tipo:{id_tipo_obra:...}
  private static Long extractIdTipoFromMap(Map<String,Object> map) {
    if (map == null) return null;
    Object v = map.get("id_tipo_obra");
    if (v instanceof Number) return ((Number)v).longValue();
    if (v instanceof String) {
      try { return Long.parseLong((String)v); } catch (NumberFormatException ignored) {}
    }
    Object tipoObj = map.get("tipo");
    if (tipoObj instanceof Map) {
      Object tid = ((Map)tipoObj).get("id_tipo_obra");
      if (tid instanceof Number) return ((Number)tid).longValue();
      if (tid instanceof String) {
        try { return Long.parseLong((String)tid); } catch (NumberFormatException ignored) {}
      }
    }
    return null;
  }

  private static String asString(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  // response helpers
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
