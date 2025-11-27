package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.auth.JwtAuthService;
import com.function.db.Db;
import com.function.model.Evento;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Azure Function HTTP para CRUD de eventos.
 * Rutas:
 *  GET  /api/eventos                -> listar (join con tipoevento, usuario, rol)
 *  GET  /api/eventos/{id}           -> obtener por id
 *  POST /api/eventos                -> crear
 *  PUT  /api/eventos/{id}           -> actualizar
 *  DELETE /api/eventos/{id}         -> eliminar (solo admin)
 *
 * Valida service-token usando JwtAuthService.
 */
public class EventosFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("eventos")
  public HttpResponseMessage handle(
      @HttpTrigger(name = "req",
                   methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                   authLevel = AuthorizationLevel.FUNCTION,
                   route = "eventos/{id?}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idPath,
      final ExecutionContext ctx) {

    try {

      String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
      try {
        JWTClaimsSet claims = com.function.auth.JwtAuthService.validate(authHeader);
      } catch (IllegalArgumentException iae) {
        return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Missing or malformed Authorization header").build();
      } catch (Exception e) {
        ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
        return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Invalid service token").build();
      }

      String method = request.getHttpMethod().name();
      switch (method) {
        case "GET":
          if (idPath == null) return listar(request);
          return obtener(request, Long.parseLong(idPath));
        case "POST":
          return crear(request);
        case "PUT":
          if (idPath == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id en ruta requerido").build();
          return actualizar(request, Long.parseLong(idPath));
        case "DELETE":
          if (idPath == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id en ruta requerido").build();
          String rolesCsv = request.getHeaders().getOrDefault("x-user-roles", request.getHeaders().get("X-User-Roles"));
          boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
          if (!isAdmin) return request.createResponseBuilder(HttpStatus.FORBIDDEN).body("Solo admin puede borrar").build();
          return eliminar(request, Long.parseLong(idPath));
        default:
          return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
      }
    } catch (Exception e) {
      ctx.getLogger().severe("Error general: " + e.getMessage());
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage())).build();
    }
  }

  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws Exception {
    String sql = "SELECT e.id_eventos, e.titulo, e.descripcion, e.fechaInicio, e.fechaTermino, e.precio, e.direccion, " +
                 "e.id_tipo_evento, te.nombre AS tipoevento_nombre, e.id_azure, u.username AS usuario_username, u.nombre_completo AS usuario_nombre, " +
                 "e.id_rol, r.nombre_rol " +
                 "FROM eventos e " +
                 "LEFT JOIN tipoevento te ON e.id_tipo_evento = te.id_tipo_evento " +
                 "LEFT JOIN usuarios u ON e.id_azure = u.id_azure " +
                 "LEFT JOIN roles r ON e.id_rol = r.id_rol " +
                 "ORDER BY e.fechaInicio DESC";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      List<Map<String,Object>> out = new ArrayList<>();
      while (rs.next()) {
        Map<String,Object> m = new HashMap<>();
        m.put("id_eventos", rs.getLong("id_eventos"));
        m.put("titulo", rs.getString("titulo"));
        m.put("descripcion", rs.getString("descripcion"));
        Timestamp fi = rs.getTimestamp("fechaInicio");
        Timestamp ft = rs.getTimestamp("fechaTermino");
        m.put("fechaInicio", fi != null ? fi.toInstant().toString() : null);
        m.put("fechaTermino", ft != null ? ft.toInstant().toString() : null);
        m.put("precio", rs.getBigDecimal("precio"));
        m.put("direccion", rs.getString("direccion"));
        m.put("id_tipo_evento", rs.getLong("id_tipo_evento"));
        m.put("tipoevento_nombre", rs.getString("tipoevento_nombre"));
        m.put("id_azure", rs.getString("id_azure"));
        m.put("usuario_username", rs.getString("usuario_username"));
        m.put("usuario_nombre", rs.getString("usuario_nombre"));
        m.put("id_rol", rs.getLong("id_rol"));
        m.put("rol_nombre", rs.getString("nombre_rol"));
        out.add(m);
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id) throws Exception {
    String sql = "SELECT e.id_eventos, e.titulo, e.descripcion, e.fechaInicio, e.fechaTermino, e.precio, e.direccion, " +
                 "e.id_tipo_evento, te.nombre AS tipoevento_nombre, e.id_azure, u.username AS usuario_username, u.nombre_completo AS usuario_nombre, " +
                 "e.id_rol, r.nombre_rol " +
                 "FROM eventos e " +
                 "LEFT JOIN tipoevento te ON e.id_tipo_evento = te.id_tipo_evento " +
                 "LEFT JOIN usuarios u ON e.id_azure = u.id_azure " +
                 "LEFT JOIN roles r ON e.id_rol = r.id_rol " +
                 "WHERE e.id_eventos = ?";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
        Map<String,Object> m = new HashMap<>();
        m.put("id_eventos", rs.getLong("id_eventos"));
        m.put("titulo", rs.getString("titulo"));
        m.put("descripcion", rs.getString("descripcion"));
        Timestamp fi = rs.getTimestamp("fechaInicio");
        Timestamp ft = rs.getTimestamp("fechaTermino");
        m.put("fechaInicio", fi != null ? fi.toInstant().toString() : null);
        m.put("fechaTermino", ft != null ? ft.toInstant().toString() : null);
        m.put("precio", rs.getBigDecimal("precio"));
        m.put("direccion", rs.getString("direccion"));
        m.put("id_tipo_evento", rs.getLong("id_tipo_evento"));
        m.put("tipoevento_nombre", rs.getString("tipoevento_nombre"));
        m.put("id_azure", rs.getString("id_azure"));
        m.put("usuario_username", rs.getString("usuario_username"));
        m.put("usuario_nombre", rs.getString("usuario_nombre"));
        m.put("id_rol", rs.getLong("id_rol"));
        m.put("rol_nombre", rs.getString("nombre_rol"));
        return json(req, m, HttpStatus.OK);
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);
    Long idTipoEvento = in.get("id_tipo_evento") != null ? ((Number)in.get("id_tipo_evento")).longValue() : null;
    String idAzure = (String) in.get("id_azure");
    Long idRol = in.get("id_rol") != null ? ((Number)in.get("id_rol")).longValue() : null;
    String titulo = (String) in.get("titulo");
    String descripcion = (String) in.get("descripcion");
    String fechaInicio = (String) in.get("fechaInicio"); // ISO string
    String fechaTermino = (String) in.get("fechaTermino");
    Number precioN = (Number) in.get("precio");
    Double precio = precioN != null ? precioN.doubleValue() : null;
    String direccion = (String) in.get("direccion");

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "INSERT INTO eventos (id_tipo_evento, id_azure, id_rol, titulo, descripcion, fechaInicio, fechaTermino, precio, direccion) VALUES (?,?,?,?,?,?,?,?,?)",
             Statement.RETURN_GENERATED_KEYS)) {

      if (idTipoEvento != null) ps.setLong(1, idTipoEvento); else ps.setNull(1, Types.BIGINT);
      ps.setString(2, idAzure);
      if (idRol != null) ps.setLong(3, idRol); else ps.setNull(3, Types.BIGINT);
      ps.setString(4, titulo);
      ps.setString(5, descripcion);
      if (fechaInicio != null) ps.setTimestamp(6, Timestamp.from(java.time.Instant.parse(fechaInicio))); else ps.setNull(6, Types.TIMESTAMP);
      if (fechaTermino != null) ps.setTimestamp(7, Timestamp.from(java.time.Instant.parse(fechaTermino))); else ps.setNull(7, Types.TIMESTAMP);
      if (precio != null) ps.setDouble(8, precio); else ps.setNull(8, Types.NUMERIC);
      ps.setString(9, direccion);
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          long newId = keys.getLong(1);
          return obtener(req, newId);
        }
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);
    Long idTipoEvento = in.get("id_tipo_evento") != null ? ((Number)in.get("id_tipo_evento")).longValue() : null;
    String idAzure = (String) in.get("id_azure");
    Long idRol = in.get("id_rol") != null ? ((Number)in.get("id_rol")).longValue() : null;
    String titulo = (String) in.get("titulo");
    String descripcion = (String) in.get("descripcion");
    String fechaInicio = (String) in.get("fechaInicio");
    String fechaTermino = (String) in.get("fechaTermino");
    Number precioN = (Number) in.get("precio");
    Double precio = precioN != null ? precioN.doubleValue() : null;
    String direccion = (String) in.get("direccion");

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "UPDATE eventos SET id_tipo_evento=?, id_azure=?, id_rol=?, titulo=?, descripcion=?, fechaInicio=?, fechaTermino=?, precio=?, direccion=? WHERE id_eventos=?")) {

      if (idTipoEvento != null) ps.setLong(1, idTipoEvento); else ps.setNull(1, Types.BIGINT);
      ps.setString(2, idAzure);
      if (idRol != null) ps.setLong(3, idRol); else ps.setNull(3, Types.BIGINT);
      ps.setString(4, titulo);
      ps.setString(5, descripcion);
      if (fechaInicio != null) ps.setTimestamp(6, Timestamp.from(java.time.Instant.parse(fechaInicio))); else ps.setNull(6, Types.TIMESTAMP);
      if (fechaTermino != null) ps.setTimestamp(7, Timestamp.from(java.time.Instant.parse(fechaTermino))); else ps.setNull(7, Types.TIMESTAMP);
      if (precio != null) ps.setDouble(8, precio); else ps.setNull(8, Types.NUMERIC);
      ps.setString(9, direccion);
      ps.setLong(10, id);

      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id) throws Exception {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM eventos WHERE id_eventos = ?")) {
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