package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.function.auth.JwtAuthService;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Azure Function HTTP para CRUD/registro de usuarios.
 * Rutas:
 *  GET  /api/usuarios            -> listar (sin password)
 *  GET  /api/usuarios/{id}       -> obtener por id (sin password)
 *  POST /api/usuarios            -> crear (body: id_azure? , id_rol, id_obra?, username, password, nombre_completo)
 *  PUT  /api/usuarios/{id}       -> actualizar (body: id_rol?, id_obra?, username?, password?, nombre_completo?)
 *  DELETE /api/usuarios/{id}     -> eliminar (solo admin según header X-User-Roles)
 *
 * Requiere JwtAuthService.validate(authHeader) para validar token de servicio.
 */
public class UsuariosFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("usuarios")
  public HttpResponseMessage handle(
      @HttpTrigger(name = "req",
                   methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                   authLevel = AuthorizationLevel.FUNCTION,
                   route = "usuarios/{id?}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idPath,
      final ExecutionContext ctx) {

    try {
      // validar token de servicio (caller = BFF app)
      String authHeader = firstNonNullHeader(request, "Authorization", "authorization");
      try {
        JWTClaimsSet svcClaims = JwtAuthService.validate(authHeader);
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
          return obtener(request, idPath);
        case "POST":
          return crear(request);
        case "PUT":
          if (idPath == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id requerido en ruta").build();
          return actualizar(request, idPath);
        case "DELETE":
          if (idPath == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id requerido en ruta").build();
          String rolesCsv = request.getHeaders().getOrDefault("x-user-roles", request.getHeaders().get("X-User-Roles"));
          boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
          if (!isAdmin) return request.createResponseBuilder(HttpStatus.FORBIDDEN).body("Solo admin puede borrar usuarios").build();
          return eliminar(request, idPath);
        default:
          return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
      }
    } catch (Exception e) {
      ctx.getLogger().severe("Error general usuarios: " + e.getMessage());
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage())).build();
    }
  }

  // LISTAR (sin password)
  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws Exception {
    String sql = "SELECT u.id_azure, u.id_rol, r.nombre_rol, u.id_obra, u.username, u.nombre_completo " +
                 "FROM usuarios u LEFT JOIN roles r ON u.id_rol = r.id_rol ORDER BY u.username";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      List<Map<String,Object>> out = new ArrayList<>();
      while (rs.next()) {
        Map<String,Object> m = new HashMap<>();
        m.put("id_azure", rs.getString("id_azure"));
        m.put("id_rol", rs.getLong("id_rol"));
        m.put("rol_nombre", rs.getString("nombre_rol"));
        m.put("id_obra", rs.getObject("id_obra") != null ? rs.getLong("id_obra") : null);
        m.put("username", rs.getString("username"));
        m.put("nombre_completo", rs.getString("nombre_completo"));
        out.add(m);
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  // OBTENER POR ID (idAzure como UUID string)
  private HttpResponseMessage obtener(HttpRequestMessage<?> req, String idAzure) throws Exception {
    String sql = "SELECT u.id_azure, u.id_rol, r.nombre_rol, u.id_obra, u.username, u.nombre_completo " +
                 "FROM usuarios u LEFT JOIN roles r ON u.id_rol = r.id_rol WHERE u.id_azure = ?";
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setObject(1, UUID.fromString(idAzure));
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("Usuario no encontrado").build();
        Map<String,Object> m = new HashMap<>();
        m.put("id_azure", rs.getString("id_azure"));
        m.put("id_rol", rs.getLong("id_rol"));
        m.put("rol_nombre", rs.getString("nombre_rol"));
        m.put("id_obra", rs.getObject("id_obra") != null ? rs.getLong("id_obra") : null);
        m.put("username", rs.getString("username"));
        m.put("nombre_completo", rs.getString("nombre_completo"));
        return json(req, m, HttpStatus.OK);
      }
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id_azure invalido").build();
    }
  }

  // CREAR (registro)
  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);

    // Campos permitidos
    String idAzureInput = (String) in.get("id_azure"); // optional
    Long idRol = in.get("id_rol") != null ? ((Number)in.get("id_rol")).longValue() : null;
    Long idObra = in.get("id_obra") != null ? ((Number)in.get("id_obra")).longValue() : null;
    String username = (String) in.get("username");
    String password = (String) in.get("password");
    String nombreCompleto = (String) in.get("nombre_completo");

    if (username == null || username.isBlank() || password == null || password.isBlank() || nombreCompleto == null || nombreCompleto.isBlank() || idRol == null) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Faltan campos obligatorios (username, password, nombre_completo, id_rol)").build();
    }

    // hash password
    String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));

    // generate or parse id_azure
    UUID idAzure = (idAzureInput != null && !idAzureInput.isBlank()) ? UUID.fromString(idAzureInput) : UUID.randomUUID();

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "INSERT INTO usuarios (id_azure, id_rol, id_obra, username, password, nombre_completo) VALUES (?,?,?,?,?,?)")) {
      ps.setObject(1, idAzure);
      ps.setLong(2, idRol);
      if (idObra != null) ps.setLong(3, idObra); else ps.setNull(3, Types.BIGINT);
      ps.setString(4, username);
      ps.setString(5, hashed);
      ps.setString(6, nombreCompleto);
      ps.executeUpdate();
      // devolver el recurso sin password
      return obtener(req, idAzure.toString());
    } catch (SQLIntegrityConstraintViolationException dup) {
      return req.createResponseBuilder(HttpStatus.CONFLICT).body("username o id_azure ya existente").build();
    } catch (SQLException sq) {
      // si la causa es unique constraint en username, manejarlo
      if (sq.getSQLState() != null && sq.getSQLState().startsWith("23")) {
        return req.createResponseBuilder(HttpStatus.CONFLICT).body("username o id_azure ya existente").build();
      }
      throw sq;
    }
  }

  // ACTUALIZAR (puede incluir cambio de password)
  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, String idAzureStr) throws Exception {
    Map<String,Object> in = MAPPER.readValue(req.getBody().orElse("{}"), Map.class);

    Long idRol = in.get("id_rol") != null ? ((Number)in.get("id_rol")).longValue() : null;
    Long idObra = in.get("id_obra") != null ? ((Number)in.get("id_obra")).longValue() : null;
    String username = (String) in.get("username");
    String password = (String) in.get("password");
    String nombreCompleto = (String) in.get("nombre_completo");

    // build dynamic update
    StringBuilder sb = new StringBuilder("UPDATE usuarios SET ");
    List<Object> params = new ArrayList<>();
    int idx = 1;
    if (idRol != null) { sb.append("id_rol = ?, "); params.add(idRol); }
    if (idObra != null) { sb.append("id_obra = ?, "); params.add(idObra); }
    if (username != null) { sb.append("username = ?, "); params.add(username); }
    if (password != null) { String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12)); sb.append("password = ?, "); params.add(hashed); }
    if (nombreCompleto != null) { sb.append("nombre_completo = ?, "); params.add(nombreCompleto); }

    if (params.isEmpty()) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Nada para actualizar").build();
    }

    // remove trailing comma
    int last = sb.lastIndexOf(", ");
    if (last >= 0) sb.delete(last, last+2);
    sb.append(" WHERE id_azure = ?");

    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(sb.toString())) {
      for (int i=0;i<params.size();i++) {
        Object p = params.get(i);
        if (p instanceof Long) ps.setLong(i+1, (Long)p);
        else ps.setObject(i+1, p);
      }
      // set id_azure param
      ps.setObject(params.size()+1, UUID.fromString(idAzureStr));
      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("Usuario no encontrado").build();
      return obtener(req, idAzureStr);
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id_azure invalido").build();
    } catch (SQLException sq) {
      if (sq.getSQLState() != null && sq.getSQLState().startsWith("23")) {
        return req.createResponseBuilder(HttpStatus.CONFLICT).body("username ya existe").build();
      }
      throw sq;
    }
  }

  // ELIMINAR
  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, String idAzureStr) throws Exception {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM usuarios WHERE id_azure = ?")) {
      ps.setObject(1, UUID.fromString(idAzureStr));
      int rows = ps.executeUpdate();
      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("id_azure invalido").build();
    }
  }

  // helpers
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
