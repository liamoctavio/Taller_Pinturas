package com.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.function.db.Db;
import com.function.exception.ApplicationException;
import com.function.model.Usuario;
import com.function.model.RolRef;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.function.auth.JwtAuthService;
import com.function.common.HttpConstants;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Azure Function HTTP para CRUD/registro de usuarios.
 * Rutas:
 * GET /api/usuarios -> listar (sin password)
 * GET /api/usuarios/{id} -> obtener por id (sin password)
 * GET /api/usuarios/{id}/obras -> listar obras asociadas por id
 * POST /api/usuarios -> crear (body: id_azure? , id_rol, username, password,
 * nombre_completo)
 * POST /api/usuarios/{id}/obras -> vincular obra a usuario
 * PUT /api/usuarios/{id} -> actualizar (body: id_rol?, username?, password?,
 * nombre_completo?)
 * DELETE /api/usuarios/{id} -> eliminar (solo admin según header X-User-Roles)
 * DELETE /api/usuarios/{id}/obras/{obraId} -> desvincular obra del usuario
 *
 * Requiere JwtAuthService.validate(authHeader) para validar token de servicio.
 */
public class UsuariosFunction {

  private static final String ID_ROL = "id_rol";
  private static final String ID_OBRA = "id_obra";
  private static final String ERROR_ID_AZURE_INVALIDO = "{\"error\":\"id_azure invalido\"}";
  private static final String NOMBRE_COMPLETO = "nombre_completo";
  private static final String USERNAME2 = "username";
  private static final String ID_AZURE = "id_azure";
  private static final String SERVICE_TOKEN_VALIDATION_FAILED = "Service token validation failed: ";
  private static final String AUTHORIZATION = "authorization";

  private record UpdateData(String sql, List<Object> params) {
  }

  private static final ObjectMapper MAPPER = JsonMapper.builder()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build();

  @FunctionName("usuariosRoot")
  public HttpResponseMessage usuariosRoot(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET,
          HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios") HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws SQLException, IOException {

    // validar token de servicio
    String authHeader = firstNonNullHeader(request, HttpConstants.AUTHORIZATION, AUTHORIZATION);
    try {
      JwtAuthService.validate(authHeader);
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_MISSING_AUTH)
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe(SERVICE_TOKEN_VALIDATION_FAILED + e.getMessage());
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_INVALID_AUTH)
          .build();
    }

    switch (request.getHttpMethod()) {
      case GET:
        return listar(request);
      case POST:
        return crear(request);
      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  @FunctionName("usuariosById")
  public HttpResponseMessage usuariosById(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.PUT,
          HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/{id}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idPath,
      final ExecutionContext ctx) throws SQLException, IOException {

    // validar token
    String authHeader = firstNonNullHeader(request, HttpConstants.AUTHORIZATION, AUTHORIZATION);
    try {
      JwtAuthService.validate(authHeader);
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_MISSING_AUTH)
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe(SERVICE_TOKEN_VALIDATION_FAILED + e.getMessage());
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_INVALID_AUTH)
          .build();
    }

    if (idPath == null || idPath.isBlank()) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"id requerido\"}").build();
    }

    String method = request.getHttpMethod().name();
    switch (method) {
      case "GET":
        return obtener(request, idPath);
      case "PUT":
        return actualizar(request, idPath);
      case "DELETE":
        return eliminar(idPath, request);
      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  @FunctionName("usuariosObrasRoot")
  public HttpResponseMessage usuariosObrasRoot(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET,
          HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/{id}/obras") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idAzure,
      final ExecutionContext ctx) throws SQLException, IOException {

    // validar service token
    String authHeader = firstNonNullHeader(request, "Authorization", AUTHORIZATION);
    try {
      JwtAuthService.validate(authHeader);
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_MISSING_AUTH)
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe(SERVICE_TOKEN_VALIDATION_FAILED + e.getMessage());
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_INVALID_AUTH)
          .build();
    }

    if (idAzure == null || idAzure.isBlank()) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"id requerido\"}").build();
    }

    String method = request.getHttpMethod().name();
    switch (method) {
      case "GET":
        return listarObrasDeUsuario(request, idAzure);
      case "POST":
        return vincularObraAUsuario(request, idAzure);
      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  @FunctionName("usuariosObrasById")
  public HttpResponseMessage usuariosObrasById(
      @HttpTrigger(name = "req", methods = {
          HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/{id}/obras/{obraId}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idAzure,
      @BindingName("obraId") String obraIdStr,
      final ExecutionContext ctx) throws SQLException {

    // validar service token
    String authHeader = firstNonNullHeader(request, "Authorization", AUTHORIZATION);
    try {
      JwtAuthService.validate(authHeader);
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_MISSING_AUTH)
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe(SERVICE_TOKEN_VALIDATION_FAILED + e.getMessage());
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
          .body(HttpConstants.ERROR_INVALID_AUTH)
          .build();
    }

    if (idAzure == null || idAzure.isBlank() || obraIdStr == null || obraIdStr.isBlank()) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"id y obraId requeridos\"}")
          .build();
    }

    long obraId;
    try {
      obraId = Long.parseLong(obraIdStr);
    } catch (NumberFormatException e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"obraId invalido\"}").build();
    }

    return desvincularObraDeUsuario(request, idAzure, obraId);
  }

  /**
   * Sincronización de Usuario (Login Silencioso).
   * Ruta: POST /api/usuarios/sync
   * Body esperado: { "id_azure": "uuid...", "username": "correo...",
   * "nombre_completo": "..." }
   */
  @FunctionName("usuariosSync")
  public HttpResponseMessage usuariosSync(
      @HttpTrigger(name = "req", methods = {
          HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/sync") HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws JsonProcessingException {

    // 1. Leer el body enviado desde Angular
    Map<String, Object> body = MAPPER.readValue(request.getBody().orElse("{}"),
        new TypeReference<Map<String, Object>>() {
        });

    String idAzureStr = asString(body.get(ID_AZURE));
    String username = asString(body.get(USERNAME2)); // Correo
    String nombre = asString(body.get(NOMBRE_COMPLETO));

    // Validar datos mínimos
    if (idAzureStr == null || username == null) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("{\"error\": \"Faltan datos (id_azure, username)\"}")
          .build();
    }

    try (Connection con = Db.connect()) {
      UUID uuid = UUID.fromString(idAzureStr);
      // 2. Verificar si el usuario YA existe
      boolean existe = false;

      try (PreparedStatement psCheck = con.prepareStatement("SELECT 1 FROM usuarios WHERE id_azure = ?")) {
        psCheck.setObject(1, uuid);
        try (ResultSet rs = psCheck.executeQuery()) {
          if (rs.next())
            existe = true;
        }
      }
      // 3. Si NO existe, lo insertamos
      if (!existe) {

        // Insertamos con contraseña "dummy" porque Azure maneja la real
        String sql = "INSERT INTO usuarios (id_azure, id_rol, username, password, nombre_completo) VALUES (?, ?, ?, 'azure-oauth', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
          ps.setObject(1, uuid);
          ps.setLong(2, 2L);
          ps.setString(3, username);
          ps.setString(4, nombre != null ? nombre : username); // Si no hay nombre, usa el correo
          ps.executeUpdate();
        }
        ctx.getLogger().log(Level.INFO, "Usuario nuevo sincronizado: {0}", username);

      } else {
        ctx.getLogger().log(Level.INFO, "Usuario ya existía: {0}", username);
      }

      return request.createResponseBuilder(HttpStatus.OK)
          .header("Content-Type", "application/json") // <--- ESTO FALTAba
          .body("{\"status\":\"synced\"}")
          .build();
    } catch (Exception e) {
      ctx.getLogger().severe("Error sync: " + e.getMessage());
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
    }
  }

  // listar (sin id_obra)
  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws SQLException, IOException {
    String sql = "SELECT u.id_azure, u.id_rol, r.nombre_rol, u.username, u.nombre_completo " +
        "FROM usuarios u LEFT JOIN roles r ON u.id_rol = r.id_rol ORDER BY u.username";
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      List<Usuario> out = new ArrayList<>();
      while (rs.next()) {
        Usuario u = mapUsuario(rs);
        out.add(u);
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  // obtener por id_azure (sin id_obra)
  private HttpResponseMessage obtener(HttpRequestMessage<?> req, String idAzure) throws SQLException, IOException {
    String sql = "SELECT u.id_azure, u.id_rol, r.nombre_rol, u.username, u.nombre_completo " +
        "FROM usuarios u LEFT JOIN roles r ON u.id_rol = r.id_rol WHERE u.id_azure = ?";
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setObject(1, UUID.fromString(idAzure));
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next())
          return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("{\"error\":\"Usuario no encontrado\"}").build();
        Usuario u = mapUsuario(rs);
        return json(req, u, HttpStatus.OK);
      }
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ERROR_ID_AZURE_INVALIDO).build();
    }
  }

  // crear (sin id_obra en usuarios)
  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws IOException {
    Map<String, Object> in = MAPPER.readValue(req.getBody().orElse("{}"), new TypeReference<Map<String, Object>>() {
    });

    String idAzureInput = asString(in.get(ID_AZURE));
    Long idRol = extractIdRolFromMap(in);
    String username = asString(in.get(USERNAME2));
    String password = asString(in.get("password"));
    String nombreCompleto = asString(in.get(NOMBRE_COMPLETO));

    if (username == null || username.isBlank() || password == null || password.isBlank() || nombreCompleto == null
        || nombreCompleto.isBlank() || idRol == null) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("{\"error\":\"Faltan campos obligatorios (username, password, nombre_completo, id_rol)\"}").build();
    }

    String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
    UUID idAzure = (idAzureInput != null && !idAzureInput.isBlank()) ? UUID.fromString(idAzureInput)
        : UUID.randomUUID();

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO usuarios (id_azure, id_rol, username, password, nombre_completo) VALUES (?,?,?,?,?)")) {
      ps.setObject(1, idAzure);
      ps.setLong(2, idRol);
      ps.setString(3, username);
      ps.setString(4, hashed);
      ps.setString(5, nombreCompleto);
      ps.executeUpdate();
      return obtener(req, idAzure.toString());
    } catch (SQLException sq) {
      if (sq.getSQLState() != null && sq.getSQLState().startsWith("23")) {
        return req.createResponseBuilder(HttpStatus.CONFLICT).body("{\"error\":\"username o id_azure ya existente\"}")
            .build();
      }
      throw new ApplicationException("Error en la creación de usuario", sq);
    }
  }

  // actualizar (sin id_obra)
  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, String idAzureStr)
      throws IOException {

    Map<String, Object> in = MAPPER.readValue(
        req.getBody().orElse("{}"),
        new TypeReference<>() {
        });

    UpdateData updateData = buildUpdateData(in);

    if (updateData.params().isEmpty()) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("{\"error\":\"Nada para actualizar\"}")
          .build();
    }

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(updateData.sql())) {

      setParams(ps, updateData.params(), idAzureStr);

      int rows = ps.executeUpdate();
      if (rows == 0) {
        return req.createResponseBuilder(HttpStatus.NOT_FOUND)
            .body("{\"error\":\"Usuario no encontrado\"}")
            .build();
      }

      return obtener(req, idAzureStr);

    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body(ERROR_ID_AZURE_INVALIDO)
          .build();
    } catch (SQLException sq) {
      if (sq.getSQLState() != null && sq.getSQLState().startsWith("23")) {
        return req.createResponseBuilder(HttpStatus.CONFLICT)
            .body("{\"error\":\"username ya existe\"}")
            .build();
      }
      throw new ApplicationException("Error en la actualización del usuario", sq);
    }
  }

  // eliminar
  private HttpResponseMessage eliminar(String idAzureStr, HttpRequestMessage<?> originalReq) throws SQLException {
    String rolesCsv = originalReq.getHeaders().getOrDefault("x-user-roles",
        originalReq.getHeaders().get("X-User-Roles"));
    boolean isAdmin = rolesCsv != null && Arrays.asList(rolesCsv.split(",")).contains("admin");
    if (!isAdmin)
      return originalReq.createResponseBuilder(HttpStatus.FORBIDDEN)
          .body("{\"error\":\"Solo admin puede borrar usuarios\"}").build();

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement("DELETE FROM usuarios WHERE id_azure = ?")) {
      ps.setObject(1, UUID.fromString(idAzureStr));
      int rows = ps.executeUpdate();
      return originalReq.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    } catch (IllegalArgumentException iae) {
      return originalReq.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ERROR_ID_AZURE_INVALIDO).build();
    }
  }

  // -----------------------
  // Relación usuarios <-> obras (endpoints auxiliares que puedes exponer con
  // rutas separadas)
  // -----------------------

  // Listar obras de un usuario: GET /api/usuarios/{id}/obras
  private HttpResponseMessage listarObrasDeUsuario(HttpRequestMessage<?> req, String idAzure)
      throws SQLException, IOException {
    String sql = "SELECT o.id_obra, o.id_tipo_obra, t.nombre AS tipo_nombre, o.titulo, o.descripcion " +
        "FROM obras o JOIN usuarios_obras uo ON o.id_obra = uo.id_obra " +
        "LEFT JOIN tipobra t ON o.id_tipo_obra = t.id_tipo_obra " +
        "WHERE uo.id_azure = ? ORDER BY uo.es_principal DESC, o.id_obra";
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setObject(1, UUID.fromString(idAzure));
      try (ResultSet rs = ps.executeQuery()) {
        List<Map<String, Object>> out = new ArrayList<>();
        while (rs.next()) {
          Map<String, Object> m = new HashMap<>();
          m.put(ID_OBRA, rs.getLong(ID_OBRA));
          m.put("id_tipo_obra", rs.getLong("id_tipo_obra"));
          m.put("tipo_nombre", rs.getString("tipo_nombre"));
          m.put("titulo", rs.getString("titulo"));
          m.put("descripcion", rs.getString("descripcion"));
          out.add(m);
        }
        return json(req, out, HttpStatus.OK);
      }
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ERROR_ID_AZURE_INVALIDO).build();
    }
  }

  // Vincular obra a usuario: POST /api/usuarios/{id}/obras
  private HttpResponseMessage vincularObraAUsuario(HttpRequestMessage<Optional<String>> req, String idAzure)
      throws SQLException, JsonProcessingException {
    Map<String, Object> body = MAPPER.readValue(req.getBody().orElse("{}"), new TypeReference<Map<String, Object>>() {
    });
    Number obraIdN = (Number) body.get(ID_OBRA);
    Boolean esPrincipal = body.get("es_principal") != null ? (Boolean) body.get("es_principal") : Boolean.FALSE;
    if (obraIdN == null)
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"id_obra requerido\"}").build();
    long obraId = obraIdN.longValue();

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO usuarios_obras (id_azure, id_obra, es_principal) VALUES (?, ?, ?) " +
                "ON CONFLICT (id_azure, id_obra) DO UPDATE SET es_principal = EXCLUDED.es_principal")) {
      ps.setObject(1, UUID.fromString(idAzure));
      ps.setLong(2, obraId);
      ps.setBoolean(3, esPrincipal);
      ps.executeUpdate();
      return req.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"id_azure o id_obra invalido\"}")
          .build();
    }
  }

  // Desvincular obra de usuario: DELETE /api/usuarios/{id}/obras/obraId
  private HttpResponseMessage desvincularObraDeUsuario(HttpRequestMessage<?> req, String idAzure, Long obraId)
      throws SQLException {
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement("DELETE FROM usuarios_obras WHERE id_azure = ? AND id_obra = ?")) {
      ps.setObject(1, UUID.fromString(idAzure));
      ps.setLong(2, obraId);
      int rows = ps.executeUpdate();
      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    } catch (IllegalArgumentException iae) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST).body("{\"error\":\"id_azure o id_obra invalido\"}")
          .build();
    }
  }

  // mapea ResultSet -> Usuario (sin password, sin id_obra)
  private static Usuario mapUsuario(ResultSet rs) throws SQLException {
    Usuario u = new Usuario();
    u.setId_azure(rs.getString(ID_AZURE));
    long rid = rs.getLong(ID_ROL);
    if (!rs.wasNull()) {
      RolRef r = new RolRef();
      r.setId_rol(rid);
      r.setNombre_rol(rs.getString("nombre_rol"));
      u.setRol(r);
      u.setId_rol(rid);
    } else {
      u.setRol(null);
      u.setId_rol(null);
    }
    u.setUsername(rs.getString(USERNAME2));
    u.setNombre_completo(rs.getString(NOMBRE_COMPLETO));
    return u;
  }

  // util helpers
  private static Long extractIdRolFromMap(Map<String, Object> map) {
    if (map == null)
      return null;
    Object v = map.get(ID_ROL);
    if (v instanceof Number number) {
      return number.longValue();
    }
    if (v instanceof String string) {
      try {
        return Long.parseLong(string);
      } catch (NumberFormatException ignored) {
        // intencional
      }
    }
    Object rolObj = map.get("rol");
    if (rolObj instanceof Map<?, ?> rolMap) {
      Object rid = rolMap.get(ID_ROL);
      if (rid instanceof Number number) {
        return number.longValue();
      }
      if (rid instanceof String string) {
        try {
          return Long.parseLong(string);
        } catch (NumberFormatException ignored) {
          // intencional
        }
      }
    }
    return null;
  }

  private static String asString(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status)
      throws IOException {
    return req.createResponseBuilder(status)
        .header("Content-Type", "application/json")
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

  private UpdateData buildUpdateData(Map<String, Object> in) {

    Long idRol = extractIdRolFromMap(in);
    String username = asString(in.get(USERNAME2));
    String password = asString(in.get("password"));
    String nombreCompleto = asString(in.get(NOMBRE_COMPLETO));

    StringBuilder sb = new StringBuilder("UPDATE usuarios SET ");
    List<Object> params = new ArrayList<>();

    if (idRol != null) {
      sb.append("id_rol = ?, ");
      params.add(idRol);
    }

    if (username != null) {
      sb.append("username = ?, ");
      params.add(username);
    }

    if (password != null) {
      sb.append("password = ?, ");
      params.add(BCrypt.hashpw(password, BCrypt.gensalt(12)));
    }

    if (nombreCompleto != null) {
      sb.append("nombre_completo = ?, ");
      params.add(nombreCompleto);
    }

    if (!params.isEmpty()) {
      sb.delete(sb.length() - 2, sb.length());
      sb.append(" WHERE id_azure = ?");
    }

    return new UpdateData(sb.toString(), params);
  }

  private void setParams(PreparedStatement ps, List<Object> params, String idAzureStr) throws SQLException {

    for (int i = 0; i < params.size(); i++) {
      Object p = params.get(i);

      if (p instanceof Long l) {
        ps.setLong(i + 1, l);
      } else {
        ps.setObject(i + 1, p);
      }
    }

    ps.setObject(params.size() + 1, UUID.fromString(idAzureStr));
  }

}