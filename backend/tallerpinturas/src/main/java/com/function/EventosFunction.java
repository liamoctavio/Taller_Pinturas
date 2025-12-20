package com.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.function.auth.JwtAuthService;
import com.function.common.HttpConstants;
import com.function.db.Db;
import com.function.dto.EventoRequestMapper;
import com.function.dto.EventoDTO;
import com.function.events.EventBusEG;
import com.function.exception.ApplicationException;
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
 * GET /api/eventos -> listar (join con tipoevento, usuario, rol)
 * GET /api/eventos/{id} -> obtener por id
 * POST /api/eventos -> crear
 * PUT /api/eventos/{id} -> actualizar
 * DELETE /api/eventos/{id} -> eliminar (solo admin)
 *
 * Valida service-token usando JwtAuthService.
 */
public class EventosFunction {

  private static final ObjectMapper MAPPER = JsonMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .build();

  // @FunctionName("eventosRoot")
  // public HttpResponseMessage eventosRoot(
  //     @HttpTrigger(name = "req", methods = { HttpMethod.GET,
  //         HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "eventos") HttpRequestMessage<Optional<String>> request,
  //     final ExecutionContext ctx) throws Exception {

  //   ctx.getLogger().info(">>> 1. INICIANDO FUNCTION EVENTOS ROOT");
    
  //   String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
    
  //   if (authHeader == null) {
  //       ctx.getLogger().severe(">>> 2. ALERTA ROJA: El Header Authorization es NULL. El BFF no lo envió.");
  //   } else {
  //       String preview = authHeader.length() > 10 ? authHeader.substring(0, 10) + "..." : authHeader;
  //       ctx.getLogger().info(">>> 2. Header recibido: " + preview);
  //   }
  //   // -------------------------

  //   try {
  //     JWTClaimsSet claims = JwtAuthService.validate(authHeader);
  //     String subject = claims.getSubject();
  //     String email = claims.getStringClaim("preferred_username");

  //     ctx.getLogger().info(">>> 3. Token VALIDADO para: " + subject + " (" + email + ")");
  //   } catch (IllegalArgumentException iae) {
  //     ctx.getLogger().severe(">>> ERROR AUTH (Argumento): " + iae.getMessage());
  //     return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  //         .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
  //         .body(HttpConstants.ERROR_MISSING_AUTH)
  //         .build();
  //   } catch (Exception e) {
  //     ctx.getLogger().severe(">>> ERROR AUTH (Validación): " + e.getMessage());
  //     e.printStackTrace(); 
  //     return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  //         .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
  //         .body("{\"error\": \"Token invalido: " + e.getMessage() + "\"}")
  //         .build();
  //   }

  //   switch (request.getHttpMethod()) {
  //     case GET:
  //       return listar(request);
  //     case POST:
  //       return crear(request);
  //     default:
  //       return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
  //   }
  // }

  @FunctionName("eventosRoot")
  public HttpResponseMessage eventosRoot(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET,
          HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "eventos") HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws IOException {

    ctx.getLogger().info(">>> 1. INICIANDO FUNCTION EVENTOS ROOT: " + request.getHttpMethod());

    // YA NO VALIDAMOS AQUÍ. DEJAMOS PASAR EL FLUJO AL SWITCH.

    switch (request.getHttpMethod()) {
      case GET:
        // ¡Público! No pide token.
        return listar(request);

      case POST:
        // ¡Privado! Validamos token antes de crear.
        if (!esTokenValido(request, ctx)) {
             return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                 .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                 .body(HttpConstants.ERROR_INVALID_AUTH).build();
        }
        return crear(request);

      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

// Valida el token y devuelve true/false SOLO LOGIN PUEDE VER EL GET LISTA
  // @FunctionName("eventosById")
  // public HttpResponseMessage eventosById(
  //     @HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.PUT,
  //         HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "eventos/{id}") HttpRequestMessage<Optional<String>> request,
  //     @BindingName("id") String idStr,
  //     final ExecutionContext ctx) throws Exception {

  //   // validar token
  //   String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
  //   try {
  //     JwtAuthService.validate(authHeader);
  //   } catch (IllegalArgumentException iae) {
  //     return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  //         .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
  //         .body(HttpConstants.ERROR_MISSING_AUTH)
  //         .build();
  //   } catch (Exception e) {
  //     ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
  //     return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  //         .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
  //         .body(HttpConstants.ERROR_INVALID_AUTH)
  //         .build();
  //   }

  //   long id;
  //   try {
  //     id = Long.parseLong(idStr);
  //   } catch (NumberFormatException e) {
  //     return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
  //         .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
  //         .body("{\"error\":\"id inválido\"}")
  //         .build();
  //   }

  //   switch (request.getHttpMethod()) {
  //     case GET:
  //       return obtener(request, id);
  //     case PUT:
  //       return actualizar(request, id);
  //     case DELETE:
  //       return eliminar(request, id);
  //     default:
  //       return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
  //   }
  // }

  @FunctionName("eventosById")
  public HttpResponseMessage eventosById(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.PUT,
          HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "eventos/{id}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idStr,
      final ExecutionContext ctx) throws IOException {

    // 1. Validamos que el ID sea numérico (esto aplica para todos)
    long id;
    try {
      id = Long.parseLong(idStr);
    } catch (NumberFormatException e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body("{\"error\":\"id inválido\"}").build();
    }

    // 2. Decidimos según el método
    switch (request.getHttpMethod()) {
      case GET:
        // ¡Público! Cualquiera puede ver el detalle.
        return obtener(request, id);

      case PUT:
        // ¡Privado! Solo usuarios logueados pueden intentar editar.
        if (!esTokenValido(request, ctx)) {
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                .body(HttpConstants.ERROR_INVALID_AUTH).build();
        }
        return actualizar(request, id);

      case DELETE:
        // ¡Privado! Solo usuarios logueados pueden intentar borrar.
        if (!esTokenValido(request, ctx)) {
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                .body(HttpConstants.ERROR_INVALID_AUTH).build();
        }
        return eliminar(request, id);

      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  // Método auxiliar para validar token solo cuando sea necesario (POST, PUT, DELETE)
  private boolean esTokenValido(HttpRequestMessage<?> request, ExecutionContext ctx) {
      String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
      
      if (authHeader == null) {
          ctx.getLogger().warning("Intento de escritura sin Header Authorization");
          return false;
      }

      try {
          JwtAuthService.validate(authHeader);
          return true; // Token OK
      } catch (Exception e) {
          ctx.getLogger().warning("Token inválido en operación de escritura: " + e.getMessage());
          return false;
      }
  }

  private HttpResponseMessage listar(HttpRequestMessage<?> req) {
    String sql = "SELECT e.id_eventos, e.titulo, e.descripcion, e.fechaInicio, e.fechaTermino, e.precio, e.direccion, "
        +
        "e.id_tipo_evento, te.nombre AS tipoevento_nombre, e.id_azure, u.username AS usuario_username, u.nombre_completo AS usuario_nombre, "
        +
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
    } catch (SQLException | IOException e) {
        throw new ApplicationException("Error listando eventos", e);
    } 
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id) {
    String sql = "SELECT e.id_eventos, e.titulo, e.descripcion, e.fechaInicio, e.fechaTermino, e.precio, e.direccion, "
        +
        "e.id_tipo_evento, te.nombre AS tipoevento_nombre, e.id_azure, u.username AS usuario_username, u.nombre_completo AS usuario_nombre, "
        +
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
        if (!rs.next())
          return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("{\"error\":\"No encontrado\"}").build();
        Evento e = mapEvento(rs);
        return json(req, e, HttpStatus.OK);
      }
    } catch (SQLException | IOException e) {
        throw new ApplicationException("Error al obtener evento", e);
    } 
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws IOException {
    Map<String, Object> in = MAPPER.readValue(req.getBody().orElse("{}"), new TypeReference<Map<String, Object>>() {});
    EventoDTO evento = EventoRequestMapper.from(in);

    // validations mínimas
    if (evento.getTitulo() == null || evento.getTitulo().isBlank()) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"titulo es obligatorio\"}").build();
    }

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO eventos (id_tipo_evento, id_azure, id_rol, titulo, descripcion, fechaInicio, fechaTermino, precio, direccion) VALUES (?,?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
          setLongOrNull(ps, 1, evento.getIdTipoEvento());
          setUUIDOrNull(ps, 2, evento.getIdAzure());
          setLongOrNull(ps, 3, evento.getIdRol());
          ps.setString(4, evento.getTitulo());
          ps.setString(5, evento.getDescripcion());
          setInstantOrNull(ps, 6, evento.getFechaInicio());
          setInstantOrNull(ps, 7, evento.getFechaTermino());
          setBigDecimalOrNull(ps, 8, evento.getPrecio());
          ps.setString(9, evento.getDireccion());
          ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          long newId = keys.getLong(1);
          EventBusEG.publish("Eventos.Evento.Creado", "/eventos/" + newId, Map.of("id_eventos", newId));
          return obtener(req, newId);
        }
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    } catch (SQLException e) {
        throw new ApplicationException("Error al obtener evento", e);
    } 
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws IOException {
    Map<String, Object> in = MAPPER.readValue(req.getBody().orElse("{}"), new TypeReference<Map<String, Object>>() {});
    EventoDTO evento = EventoRequestMapper.from(in);
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "UPDATE eventos SET id_tipo_evento=?, id_azure=?, id_rol=?, titulo=?, descripcion=?, fechaInicio=?, fechaTermino=?, precio=?, direccion=? WHERE id_eventos=?")) {
          setLongOrNull(ps, 1, evento.getIdTipoEvento()); 
          setUUIDOrNull(ps, 2, evento.getIdAzure());
          setLongOrNull(ps, 3, evento.getIdRol());
          ps.setString(4, evento.getTitulo());
          ps.setString(5, evento.getDescripcion());
          setInstantOrNull(ps, 6, evento.getFechaInicio());
          setInstantOrNull(ps, 7, evento.getFechaTermino());
          setBigDecimalOrNull(ps, 8, evento.getPrecio());
          ps.setString(9, evento.getDireccion());
          ps.setLong(10, id);
      int rows = ps.executeUpdate();
      if (rows == 0)
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      EventBusEG.publish("Eventos.Evento.Actualizado", "/eventos/" + id, Map.of("id_eventos", id));
      return obtener(req, id);
    } catch (SQLException e) {
        throw new ApplicationException("Error al obtener evento", e);
    }
  }

  // private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id, HttpRequestMessage<?> originalReq)
  //     throws Exception {
  //   // autorización: solo admin (según header X-User-Roles)
  //   String rolesCsv = originalReq.getHeaders().getOrDefault("x-user-roles",
  //       originalReq.getHeaders().get("X-User-Roles"));
  //   boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
  //   if (!isAdmin)
  //     return originalReq.createResponseBuilder(HttpStatus.FORBIDDEN).body("{\"error\":\"Solo admin puede borrar\"}")
  //         .build();

  //   try (Connection con = Db.connect();
  //       PreparedStatement ps = con.prepareStatement("DELETE FROM eventos WHERE id_eventos = ?")) {
  //     ps.setLong(1, id);
  //     int rows = ps.executeUpdate();
  //     if (rows > 0) {
  //       EventBusEG.publish("Eventos.Evento.Eliminado", "/eventos/" + id, Map.of("id_eventos", id));
  //       return originalReq.createResponseBuilder(HttpStatus.NO_CONTENT).build();
  //     } else {
  //       return originalReq.createResponseBuilder(HttpStatus.NOT_FOUND).build();
  //     }
  //   }
  // }
  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long idEvento) {
    // 1. Obtener quién quiere borrar
    String idAzureSolicitante = req.getQueryParameters().get("id_azure");

    if (idAzureSolicitante == null) {
        return req.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                  .body("{\"error\": \"Falta id_azure\"}").build();
    }

    try (Connection con = Db.connect()) {
        
        boolean esAdmin = false;
        boolean esOwner = false;

        // A. VERIFICAR SI ES ADMIN
        String sqlAdmin = "SELECT id_rol FROM usuarios WHERE id_azure = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlAdmin)) {
            ps.setObject(1, java.util.UUID.fromString(idAzureSolicitante));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getLong("id_rol") == 1) {
                    esAdmin = true;
                }
            }
        }

        // B. VERIFICAR SI ES DUEÑO (Si no es admin)
        if (!esAdmin) {
             String sqlOwner = "SELECT 1 FROM eventos WHERE id_eventos = ? AND id_azure = ?";
             try (PreparedStatement ps = con.prepareStatement(sqlOwner)) {
                ps.setLong(1, idEvento);
                ps.setObject(2, java.util.UUID.fromString(idAzureSolicitante));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) esOwner = true;
                }
             }
        }

        // C. DECISIÓN FINAL
        if (!esAdmin && !esOwner) {
            return req.createResponseBuilder(HttpStatus.FORBIDDEN)
                      .body("{\"error\": \"No tienes permiso para borrar este evento\"}").build();
        }

        // D. BORRAR
        String sqlDelete = "DELETE FROM eventos WHERE id_eventos = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
            ps.setLong(1, idEvento);
            int rows = ps.executeUpdate();
            if (rows > 0) return req.createResponseBuilder(HttpStatus.OK).body("{\"status\": \"Eliminado\"}").build();
            else return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
        }

    } catch (Exception e) {
        return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body("Error: " + e.getMessage()).build();
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
    e.setId_azure(idAzure); // esto para que el BFF lo vea
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
  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status)
      throws IOException {
    return req.createResponseBuilder(status)
        .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
        .body(MAPPER.writeValueAsString(body))
        .build();
  }

  private static String firstNonNullHeader(HttpRequestMessage<?> req, String... names) {
    for (String n : names) {
      String v = req.getHeaders().get(n);
      if (v != null)
        return v;
    }
    return null;
  }

  private void setLongOrNull(PreparedStatement ps, int idx, Long value) throws SQLException {
  if (value != null) ps.setLong(idx, value);
  else ps.setNull(idx, Types.BIGINT);
  }

  private void setUUIDOrNull(PreparedStatement ps, int idx, UUID value) throws SQLException {
    if (value != null) ps.setObject(idx, value);
    else ps.setNull(idx, Types.OTHER);
  }

  private void setInstantOrNull(PreparedStatement ps, int idx, Instant value) throws SQLException {
    if (value != null) ps.setTimestamp(idx, Timestamp.from(value));
    else ps.setNull(idx, Types.TIMESTAMP);
  }

  private void setBigDecimalOrNull(PreparedStatement ps, int idx, BigDecimal value) throws SQLException {
    if (value != null) ps.setBigDecimal(idx, value);
    else ps.setNull(idx, Types.NUMERIC);
  }


}
