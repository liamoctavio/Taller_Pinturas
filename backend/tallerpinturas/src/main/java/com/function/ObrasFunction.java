package com.function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.function.common.HttpConstants;
import com.function.db.Db;
import com.function.dto.ObraDTO;
import com.function.dto.ObraRequestMapper;
import com.function.events.EventBusEG;
import com.function.exception.ApplicationException;
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
 * GET /api/obras -> listar (sin imagen)
 * GET /api/obras/{id}?includeImage=true -> obtener con imagen en base64
 * POST /api/obras -> crear (body incluye imagenBase64 opcional)
 * PUT /api/obras/{id} -> actualizar (body incluye imagenBase64 opcional)
 * DELETE /api/obras/{id} -> eliminar (solo admin según header X-User-Roles)
 *
 * Nota: acepta input flexible en POST/PUT:
 * - { "id_tipo_obra": 1, "titulo":"...", "descripcion":"...",
 * "imagenBase64":"..." }
 * - o { "tipo": { "id_tipo_obra": 1 }, "titulo":"...", ... }
 * 
 * Requiere JwtAuthService.validate(authHeader) para validar token de servicio.
 */
public class ObrasFunction {

  private static final String ID_TIPO_OBRA = "id_tipo_obra";
  private static final String ID_AZURE = "id_azure";
  private static final String DESCRIPCION = "descripcion";
  private static final String TITULO = "titulo";
  private static final String ID_OBRA = "id_obra";
  private static final ObjectMapper MAPPER = JsonMapper.builder()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build();

  // @FunctionName("obrasRoot")
  // public HttpResponseMessage obrasRoot(
  // @HttpTrigger(name = "req",
  // methods = {HttpMethod.GET, HttpMethod.POST},
  // authLevel = AuthorizationLevel.ANONYMOUS,
  // route = "obras")
  // HttpRequestMessage<Optional<String>> request,
  // final ExecutionContext ctx) throws Exception {

  // // validar token de servicio
  // String authHeader = firstNonNullHeader(request, "Authorization",
  // "authorization");
  // try {
  // JWTClaimsSet claims = com.function.auth.JwtAuthService.validate(authHeader);
  // // opcional: usar claims si necesario
  // } catch (IllegalArgumentException iae) {
  // return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"Missing or malformed Authorization header\"}")
  // .build();
  // } catch (Exception e) {
  // ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
  // return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"Invalid service token\"}")
  // .build();
  // }

  // switch (request.getHttpMethod()) {
  // case GET: return listar(request);
  // case POST: return crear(request, ctx);
  // default: return
  // request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
  // }
  // }
  @FunctionName("obrasRoot")
  public HttpResponseMessage obrasRoot(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET,
          HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "obras") HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) {

    // YA NO VALIDAMOS AQUÍ ARRIBA.

    switch (request.getHttpMethod()) {
      case GET:
        // ¡Público!
        return listar(request);

      case POST:
        // ¡Privado! Validamos token antes de crear
        if (!esTokenValido(request, ctx)) {
          return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
              .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
              .body("{\"error\":\"Missing or malformed Authorization header\"}")
              .build();
        }
        return crear(request, ctx);

      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  // @FunctionName("obrasById")
  // public HttpResponseMessage obrasById(
  // @HttpTrigger(name = "req",
  // methods = {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
  // authLevel = AuthorizationLevel.ANONYMOUS,
  // route = "obras/{id}")
  // HttpRequestMessage<Optional<String>> request,
  // @BindingName("id") String idStr,
  // final ExecutionContext ctx) throws Exception {

  // // validar token de servicio
  // String authHeader = firstNonNullHeader(request, "Authorization",
  // "authorization");
  // try {
  // com.function.auth.JwtAuthService.validate(authHeader);
  // } catch (IllegalArgumentException iae) {
  // return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"Missing or malformed Authorization header\"}")
  // .build();
  // } catch (Exception e) {
  // ctx.getLogger().severe("Service token validation failed: " + e.getMessage());
  // return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"Invalid service token\"}")
  // .build();
  // }

  // long id;
  // try { id = Long.parseLong(idStr); }
  // catch (NumberFormatException e) {
  // return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
  // .body("{\"error\":\"id inválido\"}")
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON).build();
  // }

  // switch (request.getHttpMethod()) {
  // case GET: return obtener(request, id);
  // case PUT: return actualizar(request, id, ctx);
  // case DELETE: return eliminar(request, id);
  // default: return
  // request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
  // }
  // }
  @FunctionName("obrasById")
  public HttpResponseMessage obrasById(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.PUT,
          HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "obras/{id}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idStr,
      final ExecutionContext ctx) throws SQLException, IOException {

    // 1. Validar ID primero (común para todos)
    long id;
    try {
      id = Long.parseLong(idStr);
    } catch (NumberFormatException e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("{\"error\":\"id inválido\"}")
          .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON).build();
    }

    // 2. Switch con validación interna
    switch (request.getHttpMethod()) {
      case GET:
        // ¡Público!
        return obtener(request, id);

      case PUT:
        // ¡Privado!
        if (!esTokenValido(request, ctx)) {
          return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
              .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
              .body("{\"error\":\"Invalid service token\"}")
              .build();
        }
        return actualizar(request, id, ctx);

      case DELETE:
        // ¡Privado!
        if (!esTokenValido(request, ctx)) {
          return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
              .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
              .body("{\"error\":\"Invalid service token\"}")
              .build();
        }
        return eliminar(request, id);

      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  // Helper para validar token bajo demanda
  private boolean esTokenValido(HttpRequestMessage<?> request, ExecutionContext ctx) {
    String authHeader = firstNonNullHeader(request, "Authorization", "authorization");

    if (authHeader == null)
      return false;

    try {
      com.function.auth.JwtAuthService.validate(authHeader);
      return true;
    } catch (Exception e) {
      ctx.getLogger().warning("Token validation failed: " + e.getMessage());
      return false;
    }
  }

  // LISTAR (sin imagen por defecto)
  // private HttpResponseMessage listar(HttpRequestMessage<?> req) throws
  // SQLException, IOException {
  // String sql = "SELECT o.id_obra, o.id_tipo_obra, t.nombre AS tipo_nombre,
  // o.titulo, o.descripcion " +
  // "FROM obras o LEFT JOIN tipobra t ON o.id_tipo_obra = t.id_tipo_obra ORDER BY
  // o.id_obra";
  // try (Connection con = Db.connect();
  // PreparedStatement ps = con.prepareStatement(sql);
  // ResultSet rs = ps.executeQuery()) {
  // List<Obra> out = new ArrayList<>();
  // while (rs.next()) {
  // out.add(map(rs, false));
  // }
  // return json(req, out, HttpStatus.OK);
  // }
  // }

  // listar los usuarios con la nuevas tablas
  private HttpResponseMessage listar(HttpRequestMessage<?> req) {
    // CAMBIO CLAVE: Hacemos JOIN para traer el id_azure del dueño
    String sql = "SELECT o.id_obra, o.titulo, o.descripcion, o.id_tipo_obra, uo.id_azure " +
        "FROM obras o " +
        "LEFT JOIN usuarios_obras uo ON o.id_obra = uo.id_obra " +
        "ORDER BY o.id_obra DESC";

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

      List<Map<String, Object>> out = new ArrayList<>();
      while (rs.next()) {
        Map<String, Object> m = new HashMap<>();
        m.put(ID_OBRA, rs.getLong(ID_OBRA));
        m.put(TITULO, rs.getString(TITULO));
        m.put(DESCRIPCION, rs.getString(DESCRIPCION));
        // ... otros campos ...

        // AHORA SÍ ENVIAMOS EL DUEÑO AL FRONTEND
        m.put(ID_AZURE, rs.getString(ID_AZURE));

        out.add(m);
      }
      return json(req, out, HttpStatus.OK);
    } catch (SQLException | IOException e) {
      throw new ApplicationException("Error listando obras", e);
    }
    // ... catch ...
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
            .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
            .body("{\"error\":\"No encontrado\"}")
            .build();
      }
    }
  }

  // CREAR - acepta input flexible antiguo
  // private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req,
  // ExecutionContext ctx) {
  // try {
  // String body = req.getBody().orElse("");
  // if (body.isBlank()) {
  // return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"Body vacío\"}")
  // .build();
  // }

  // // parse raw into Map first to allow flexible input (id_tipo_obra or
  // tipo:{id_tipo_obra})
  // Map<String,Object> inMap = MAPPER.readValue(body, Map.class);

  // Long idTipo = extractIdTipoFromMap(inMap);
  // String titulo = asString(inMap.get("titulo"));
  // String descripcion = asString(inMap.get("descripcion"));
  // String imagenBase64 = asString(inMap.get("imagenBase64"));
  // byte[] imageBytes = (imagenBase64 != null && !imagenBase64.isBlank()) ?
  // Base64.getDecoder().decode(imagenBase64) : null;

  // if (titulo == null || titulo.isBlank()) {
  // return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"titulo es obligatorio\"}")
  // .build();
  // }

  // try (Connection con = Db.connect();
  // PreparedStatement ps = con.prepareStatement(
  // "INSERT INTO obras (id_tipo_obra, titulo, descripcion, imagen) VALUES
  // (?,?,?,?)",
  // Statement.RETURN_GENERATED_KEYS)) {

  // if (idTipo != null) ps.setLong(1, idTipo); else ps.setNull(1, Types.BIGINT);
  // ps.setString(2, titulo);
  // ps.setString(3, descripcion);
  // if (imageBytes != null) ps.setBytes(4, imageBytes); else ps.setNull(4,
  // Types.BINARY);

  // int rows = ps.executeUpdate();
  // if (rows > 0) {
  // try (ResultSet keys = ps.getGeneratedKeys()) {
  // if (keys.next()) {
  // long newId = keys.getLong(1);
  // try {
  // Map<String,Object> data = new HashMap<>();
  // data.put("id_obra", newId);
  // data.put("titulo", titulo);
  // EventBusEG.publish("Arte.Obra.Creada", "/obras/" + newId, data);
  // } catch (Throwable t) {
  // ctx.getLogger().info("EventBus publish failed (ignored): " + t.getMessage());
  // }
  // return obtener(req, newId);
  // }
  // }
  // }
  // return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"Insert no afectó filas\"}")
  // .build();
  // }
  // } catch (com.fasterxml.jackson.databind.JsonMappingException jm) {
  // return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"JSON inválido\",\"detalle\":\"" +
  // jm.getOriginalMessage().replace("\"","'") + "\"}")
  // .build();
  // } catch (SQLException ex) {
  // return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"DB\",\"sqlstate\":\"" + ex.getSQLState() +
  // "\",\"code\":" + ex.getErrorCode() +
  // ",\"message\":\"" + ex.getMessage().replace("\"","'") + "\"}")
  // .build();
  // } catch (Exception e) {
  // return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
  // .header(HttpConstants.CONTENT_TYPE,HttpConstants.APPLICATION_JSON)
  // .body("{\"error\":\"server\",\"message\":\"" +
  // e.getMessage().replace("\"","'") + "\"}")
  // .build();
  // }
  // }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req, ExecutionContext ctx) {
    try {
      String body = req.getBody().orElse("");
      if (body.isBlank()) {
        return badRequest(req, "Body vacío");
      }
      Map<String, Object> inMap = MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {
      });
      ObraDTO obra = ObraRequestMapper.from(inMap);
      try (Connection con = Db.connect()) {
        long newId = insertarObra(con, obra);
        vincularUsuarioObra(con, obra.getIdAzure(), newId);
        return obtener(req, newId);
      }
    } catch (ApplicationException e) {
      return internalError(req, e.getMessage());
    } catch (Exception e) {
      ctx.getLogger().info("EventBus publish failed (ignored): " + e.getMessage());
      return internalError(req, "Error interno");
    }
  }

  // ACTUALIZAR - acepta input flexible similar a crear
  private HttpResponseMessage actualizar(
      HttpRequestMessage<Optional<String>> req,
      long id, ExecutionContext ctx) throws IOException {

    String body = req.getBody().orElse("{}");
    Map<String, Object> inMap = MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {
    });

    ObraDTO obra = ObraRequestMapper.from(inMap);
    byte[] imageBytes = (obra.getImagen() != null && !obra.getImagen().isBlank())
        ? Base64.getDecoder().decode(obra.getImagen())
        : null;
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "UPDATE obras SET id_tipo_obra = ?, titulo = ?, descripcion = ?, imagen = COALESCE(?, imagen) WHERE id_obra = ?")) {

      if (obra.getIdTipo() != null)
        ps.setLong(1, obra.getIdTipo());
      else
        ps.setNull(1, Types.BIGINT);

      ps.setString(2, obra.getTitulo());
      ps.setString(3, obra.getDescripcion());

      if (imageBytes != null)
        ps.setBytes(4, imageBytes);
      else
        ps.setNull(4, Types.BINARY);

      ps.setLong(5, id);

      int rows = ps.executeUpdate();
      if (rows == 0) {
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      }

      publicarEventoActualizacion(id, obra, ctx);
      return obtener(req, id);

    } catch (SQLException e) {
      throw new ApplicationException("Error en la actualización de obra", e);
    }
  }

  // ELImina pero el admin no puede
  // private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long idObra)
  // throws Exception {

  // // 1. Obtener quién quiere borrar (desde el Query Param que enviamos en
  // Angular)
  // String idAzureSolicitante = req.getQueryParameters().get("id_azure");

  // // 2. Obtener Roles (para saber si es Admin) - Esto viene en los Headers
  // usualmente
  // // Si no usas headers de roles, asumiremos que validamos por base de datos
  // abajo.
  // String rolesHeader = req.getHeaders().getOrDefault("x-user-roles", "");
  // boolean esAdmin = rolesHeader.contains("admin"); // O lógica similar si
  // tienes roles implementados

  // if (idAzureSolicitante == null) {
  // return req.createResponseBuilder(HttpStatus.UNAUTHORIZED)
  // .body("{\"error\": \"Falta id_azure en la petición\"}").build();
  // }

  // try (Connection con = Db.connect()) {

  // // PASO A: Verificar si tiene permiso
  // // El permiso se concede si:
  // // 1. Es Admin (lo verificamos por rol o consulta)
  // // 2. O SI EXISTE un vínculo en usuarios_obras entre este usuario y esta obra

  // boolean esOwner = false;

  // // Verificamos en la tabla de unión si este usuario es dueño de esta obra
  // String sqlCheck = "SELECT 1 FROM usuarios_obras WHERE id_obra = ? AND
  // id_azure = ?";
  // try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
  // psCheck.setLong(1, idObra);
  // psCheck.setObject(2, java.util.UUID.fromString(idAzureSolicitante));
  // try (ResultSet rs = psCheck.executeQuery()) {
  // if (rs.next()) {
  // esOwner = true;
  // }
  // }
  // }

  // // Si NO es dueño y NO es admin, rechazamos
  // // (Si aún no tienes roles configurados en headers, confía solo en esOwner
  // por ahora)
  // if (!esOwner && !esAdmin) {
  // return req.createResponseBuilder(HttpStatus.FORBIDDEN)
  // .body("{\"error\": \"No tienes permiso para borrar esta obra (no es
  // tuya)\"}")
  // .build();
  // }

  // // PASO B: Proceder a borrar
  // // Como tienes ON DELETE CASCADE en tu SQL, al borrar la obra se borra el
  // vínculo solo.
  // String sqlDelete = "DELETE FROM obras WHERE id_obra = ?";
  // try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
  // ps.setLong(1, idObra);
  // int rows = ps.executeUpdate();

  // if (rows > 0) {
  // return req.createResponseBuilder(HttpStatus.OK)
  // .body("{\"status\": \"Eliminado\"}").build();
  // } else {
  // return req.createResponseBuilder(HttpStatus.NOT_FOUND)
  // .body("{\"error\": \"Obra no encontrada\"}").build();
  // }
  // }
  // } catch (Exception e) {
  // return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
  // .body("{\"error\": \"" + e.getMessage() + "\"}").build();
  // }
  // }

  // eliminar con verificacion de admin y dueño
  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long idObra) {

    // 1. Obtener quién quiere borrar
    String idAzureSolicitante = req.getQueryParameters().get(ID_AZURE);

    if (idAzureSolicitante == null) {
      return req.createResponseBuilder(HttpStatus.UNAUTHORIZED)
          .body("{\"error\": \"Falta id_azure en la petición\"}").build();
    }

    try (Connection con = Db.connect()) {

      // =====================================================================
      // PASO 1: VERIFICAR SI ES ADMIN (CONSULTANDO LA BD)
      // =====================================================================
      boolean esAdmin = false;
      String sqlAdmin = "SELECT id_rol FROM usuarios WHERE id_azure = ?";
      try (PreparedStatement psAdmin = con.prepareStatement(sqlAdmin)) {
        psAdmin.setObject(1, java.util.UUID.fromString(idAzureSolicitante));
        try (ResultSet rsAdmin = psAdmin.executeQuery()) {
          if (rsAdmin.next()) {
            long rol = rsAdmin.getLong("id_rol");
            if (rol == 1) {
              esAdmin = true;
            }
          }
        }
      }

      // =====================================================================
      // PASO 2: SI NO ES ADMIN, VERIFICAR SI ES DUEÑO
      // =====================================================================
      boolean esOwner = false;
      if (!esAdmin) { // Solo gastamos recursos buscando si no es admin ya
        String sqlCheck = "SELECT 1 FROM usuarios_obras WHERE id_obra = ? AND id_azure = ?";
        try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
          psCheck.setLong(1, idObra);
          psCheck.setObject(2, java.util.UUID.fromString(idAzureSolicitante));
          try (ResultSet rs = psCheck.executeQuery()) {
            if (rs.next()) {
              esOwner = true;
            }
          }
        }
      }

      // =====================================================================
      // PASO 3: DECISIÓN FINAL
      // =====================================================================
      if (!esOwner && !esAdmin) {
        return req.createResponseBuilder(HttpStatus.FORBIDDEN)
            .body("{\"error\": \"No tienes permiso. No eres el dueño ni Admin.\"}")
            .build();
      }

      // PASO 4: BORRAR
      String sqlDelete = "DELETE FROM obras WHERE id_obra = ?";
      try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
        ps.setLong(1, idObra);
        int rows = ps.executeUpdate();

        if (rows > 0) {
          return req.createResponseBuilder(HttpStatus.OK)
              .body("{\"status\": \"Eliminado\"}").build();
        } else {
          return req.createResponseBuilder(HttpStatus.NOT_FOUND)
              .body("{\"error\": \"Obra no encontrada\"}").build();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("{\"error\": \"" + e.getMessage() + "\"}").build();
    }
  }

  // Helper: map ResultSet -> Obra (con TipoObra anidado)
  private static Obra map(ResultSet rs, boolean includeImage) throws SQLException {
    Obra o = new Obra();

    long id = rs.getLong(ID_OBRA);
    if (!rs.wasNull())
      o.setId_obra(id);

    Long tid = null;
    long tmpTid = rs.getLong(ID_TIPO_OBRA);
    if (!rs.wasNull())
      tid = tmpTid;

    String tipoNombre = rs.getString("tipo_nombre"); // may be null

    if (tid != null || tipoNombre != null) {
      TipoObra t = new TipoObra();
      t.setId_tipo_obra(tid);
      t.setNombre(tipoNombre);
      o.setTipo(t);
    } else {
      o.setTipo(null);
    }

    o.setTitulo(rs.getString(TITULO));
    o.setDescripcion(rs.getString(DESCRIPCION));

    if (includeImage) {
      byte[] b = rs.getBytes("imagen");
      if (b != null)
        o.setImagenBase64(Base64.getEncoder().encodeToString(b));
      else
        o.setImagenBase64(null);
    }

    return o;
  }

  // response helpers
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

  // helper insertar obra

  private long insertarObra(Connection con, ObraDTO data) throws SQLException {
    String sql = "INSERT INTO obras (id_tipo_obra, titulo, descripcion, imagen) VALUES (?,?,?,?)";
    byte[] imageBytes = (data.getImagen() != null && !data.getImagen().isBlank())
        ? Base64.getDecoder().decode(data.getImagen())
        : null;
    try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      if (data.getIdTipo() != null)
        ps.setLong(1, data.getIdTipo());
      else
        ps.setNull(1, Types.BIGINT);
      ps.setString(2, data.getTitulo());
      ps.setString(3, data.getDescripcion());
      if (imageBytes != null)
        ps.setBytes(4, imageBytes);
      else
        ps.setNull(4, Types.BINARY);

      if (ps.executeUpdate() == 0) {
        throw new ApplicationException("No se insertó la obra");
      }

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next())
          return keys.getLong(1);
      }
      throw new ApplicationException("No se generó ID");
    }
  }

  private void vincularUsuarioObra(Connection con, UUID idAzure, long idObra) {
    if (idAzure == null)
      return;

    String sql = "INSERT INTO usuarios_obras (id_azure, id_obra) VALUES (?, ?)";

    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setObject(1, idAzure);
      ps.setLong(2, idObra);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new ApplicationException("No se pudo vincular usuario con obra", e);
    }
  }

  private HttpResponseMessage badRequest(HttpRequestMessage<?> req, String msg) {
    return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
        .body("{\"error\":\"" + msg + "\"}")
        .build();
  }

  private HttpResponseMessage internalError(HttpRequestMessage<?> req, String msg) {
    return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("{\"error\":\"" + msg + "\"}")
        .build();
  }

  private void publicarEventoActualizacion(long id, ObraDTO obra, ExecutionContext ctx) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put(ID_OBRA, id);
      data.put(TITULO, obra.getTitulo());

      EventBusEG.publish(
          "Arte.Obra.Actualizada",
          "/obras/" + id,
          data);
    } catch (Exception e) {
      ctx.getLogger().info("EventBus publish failed (ignored): " + e.getMessage());
    }
  }

}
