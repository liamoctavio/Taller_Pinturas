package com.function;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.function.events.EventBusEG;
import com.function.model.Evento;
import com.function.model.TipoEvento;
import com.function.model.UsuarioRef;
import com.function.model.RolRef;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;

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

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

  @FunctionName("eventosRoot")
  public HttpResponseMessage eventosRoot(
      @HttpTrigger(name = "req",
          methods = {HttpMethod.GET, HttpMethod.POST},
          authLevel = AuthorizationLevel.FUNCTION,
          route = "eventos")
      HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws Exception {

    // validar token de servicio
    String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
    try {
      JWTClaimsSet claims = com.function.auth.JwtAuthService.validate(authHeader);
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
      case POST: return crear(request);
      default:   return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  @FunctionName("eventosById")
  public HttpResponseMessage eventosById(
      @HttpTrigger(name = "req",
          methods = {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
          authLevel = AuthorizationLevel.FUNCTION,
          route = "eventos/{id}")
      HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idStr,
      final ExecutionContext ctx) throws Exception {

    // validar token
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
          .header("Content-Type","application/json")
          .body("{\"error\":\"id inválido\"}")
          .build();
    }

    switch (request.getHttpMethod()) {
      case GET:    return obtener(request, id);
      case PUT:    return actualizar(request, id);
      case DELETE: return eliminar(request, id, request);
      default:     return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
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
      List<Evento> out = new ArrayList<>();
      while (rs.next()) {
        out.add(mapEvento(rs));
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
        if (!rs.next()) return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("{\"error\":\"No encontrado\"}").build();
        Evento e = mapEvento(rs);
        return json(req, e, HttpStatus.OK);
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);

    Long idTipoEvento = extractIdTipoEventoFromMap(in);
    String idAzure = asString(in.get("id_azure"));
    Long idRol = in.get("id_rol") != null ? ((Number)in.get("id_rol")).longValue() : null;
    String titulo = asString(in.get("titulo"));
    String descripcion = asString(in.get("descripcion"));
    String fechaInicio = asString(in.get("fechaInicio")); // ISO
    String fechaTermino = asString(in.get("fechaTermino"));
    Number precioN = (Number) in.get("precio");
    BigDecimal precio = precioN != null ? new BigDecimal(precioN.toString()) : null;
    String direccion = asString(in.get("direccion"));

    // validations mínimas
    if (titulo == null || titulo.isBlank()) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"titulo es obligatorio\"}").build();
    }

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "INSERT INTO eventos (id_tipo_evento, id_azure, id_rol, titulo, descripcion, fechaInicio, fechaTermino, precio, direccion) VALUES (?,?,?,?,?,?,?,?,?)",
             Statement.RETURN_GENERATED_KEYS)) {

      if (idTipoEvento != null) ps.setLong(1, idTipoEvento); else ps.setNull(1, Types.BIGINT);
      ps.setString(2, idAzure);
      if (idRol != null) ps.setLong(3, idRol); else ps.setNull(3, Types.BIGINT);
      ps.setString(4, titulo);
      ps.setString(5, descripcion);
      if (fechaInicio != null) ps.setTimestamp(6, Timestamp.from(Instant.parse(fechaInicio))); else ps.setNull(6, Types.TIMESTAMP);
      if (fechaTermino != null) ps.setTimestamp(7, Timestamp.from(Instant.parse(fechaTermino))); else ps.setNull(7, Types.TIMESTAMP);
      if (precio != null) ps.setBigDecimal(8, precio); else ps.setNull(8, Types.NUMERIC);
      ps.setString(9, direccion);
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          long newId = keys.getLong(1);
          EventBusEG.publish("Eventos.Evento.Creado", "/eventos/" + newId, Map.of("id_eventos", newId));
          return obtener(req, newId);
        }
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);

    Long idTipoEvento = extractIdTipoEventoFromMap(in);
    String idAzure = asString(in.get("id_azure"));
    Long idRol = in.get("id_rol") != null ? ((Number)in.get("id_rol")).longValue() : null;
    String titulo = asString(in.get("titulo"));
    String descripcion = asString(in.get("descripcion"));
    String fechaInicio = asString(in.get("fechaInicio"));
    String fechaTermino = asString(in.get("fechaTermino"));
    Number precioN = (Number) in.get("precio");
    BigDecimal precio = precioN != null ? new BigDecimal(precioN.toString()) : null;
    String direccion = asString(in.get("direccion"));

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "UPDATE eventos SET id_tipo_evento=?, id_azure=?, id_rol=?, titulo=?, descripcion=?, fechaInicio=?, fechaTermino=?, precio=?, direccion=? WHERE id_eventos=?")) {

      if (idTipoEvento != null) ps.setLong(1, idTipoEvento); else ps.setNull(1, Types.BIGINT);
      ps.setString(2, idAzure);
      if (idRol != null) ps.setLong(3, idRol); else ps.setNull(3, Types.BIGINT);
      ps.setString(4, titulo);
      ps.setString(5, descripcion);
      if (fechaInicio != null) ps.setTimestamp(6, Timestamp.from(Instant.parse(fechaInicio))); else ps.setNull(6, Types.TIMESTAMP);
      if (fechaTermino != null) ps.setTimestamp(7, Timestamp.from(Instant.parse(fechaTermino))); else ps.setNull(7, Types.TIMESTAMP);
      if (precio != null) ps.setBigDecimal(8, precio); else ps.setNull(8, Types.NUMERIC);
      ps.setString(9, direccion);
      ps.setLong(10, id);

      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();

      EventBusEG.publish("Eventos.Evento.Actualizado", "/eventos/" + id, Map.of("id_eventos", id));
      return obtener(req, id);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id, HttpRequestMessage<?> originalReq) throws Exception {
    // autorización: solo admin (según header X-User-Roles)
    String rolesCsv = originalReq.getHeaders().getOrDefault("x-user-roles", originalReq.getHeaders().get("X-User-Roles"));
    boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
    if (!isAdmin) return originalReq.createResponseBuilder(HttpStatus.FORBIDDEN).body("{\"error\":\"Solo admin puede borrar\"}").build();

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM eventos WHERE id_eventos = ?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();
      if (rows > 0) {
        EventBusEG.publish("Eventos.Evento.Eliminado", "/eventos/" + id, Map.of("id_eventos", id));
        return originalReq.createResponseBuilder(HttpStatus.NO_CONTENT).build();
      } else {
        return originalReq.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      }
    }
  }

  // mapea ResultSet -> Evento con TipoEvento, UsuarioRef y RolRef
  private static Evento mapEvento(ResultSet rs) throws SQLException {
    Evento e = new Evento();
    e.setId_eventos(rs.getLong("id_eventos"));
    e.setTitulo(rs.getString("titulo"));
    e.setDescripcion(rs.getString("descripcion"));

    Timestamp fi = rs.getTimestamp("fechaInicio");
    Timestamp ft = rs.getTimestamp("fechaTermino");
    e.setFechaInicio(fi != null ? fi.toInstant().toString() : null);
    e.setFechaTermino(ft != null ? ft.toInstant().toString() : null);
    e.setPrecio(rs.getBigDecimal("precio"));
    e.setDireccion(rs.getString("direccion"));

    long tid = rs.getLong("id_tipo_evento");
    TipoEvento te = null;
    if (!rs.wasNull() || rs.getString("tipoevento_nombre") != null) {
      te = new TipoEvento();
      te.setId_tipo_evento(!rs.wasNull() ? tid : null);
      te.setNombre(rs.getString("tipoevento_nombre"));
    }
    e.setTipo(te);

    String idAzure = rs.getString("id_azure");
    if (idAzure != null) {
      UsuarioRef u = new UsuarioRef();
      u.setId_azure(idAzure);
      u.setUsername(rs.getString("usuario_username"));
      u.setNombre_completo(rs.getString("usuario_nombre"));
      e.setUsuario(u);
    } else {
      e.setUsuario(null);
    }

    long rid = rs.getLong("id_rol");
    if (!rs.wasNull() || rs.getString("nombre_rol") != null) {
      RolRef r = new RolRef();
      r.setId_rol(!rs.wasNull() ? rid : null);
      r.setNombre_rol(rs.getString("nombre_rol"));
      e.setRol(r);
    } else {
      e.setRol(null);
    }

    return e;
  }

  // helpers
  private static Long extractIdTipoEventoFromMap(Map<String,Object> map) {
    if (map == null) return null;
    Object v = map.get("id_tipo_evento");
    if (v instanceof Number) return ((Number)v).longValue();
    if (v instanceof String) {
      try { return Long.parseLong((String)v); } catch (NumberFormatException ignored) {}
    }
    Object tipoObj = map.get("tipo");
    if (tipoObj instanceof Map) {
      Object tid = ((Map)tipoObj).get("id_tipo_evento");
      if (tid instanceof Number) return ((Number)tid).longValue();
      if (tid instanceof String) {
        try { return Long.parseLong((String)tid); } catch (NumberFormatException ignored) {}
      }
    }
    return null;
  }

  private static String asString(Object o) { return o == null ? null : String.valueOf(o); }

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
