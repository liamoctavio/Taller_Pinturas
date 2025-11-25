package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.auth.JwtAuthService;
import com.function.db.Db;
import com.function.model.Obra;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class ObrasFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("obras")
  public HttpResponseMessage handle(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
          HttpMethod.DELETE }, authLevel = AuthorizationLevel.FUNCTION, route = "obras/{id?}") HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String id,
      final ExecutionContext ctx) throws Exception {
    String authHeader = request.getHeaders().getOrDefault("Authorization", request.getHeaders().get("authorization"));
    try {
      JWTClaimsSet claims = JwtAuthService.validate(authHeader);
      String userId = request.getHeaders().get("x-user-id");
      String rolesCsv = request.getHeaders().get("x-user-roles");
      List<String> roles = rolesCsv == null ? List.of() : List.of(rolesCsv.split(","));
      boolean isAdmin = roles.contains("admin");

      if ("DELETE".equals(request.getHttpMethod().name()) && !isAdmin) {
        return request.createResponseBuilder(HttpStatus.FORBIDDEN).body("Solo admin puede borrar").build();
      }

      String method = request.getHttpMethod().name();
      switch (method) {
        case "GET":
          return (id == null) ? listar(request)
              : obtener(request, Long.parseLong(id), request.getQueryParameters().get("includeImage"));
        case "POST":
          return crear(request);
        case "PUT":
          return actualizar(request, Long.parseLong(id));
        case "DELETE":
          return eliminar(request, Long.parseLong(id));
        default:
          return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();

      }
    } catch (IllegalArgumentException iae) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Missing token").build();
    } catch (BadJWTException be) {
      return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Token invalido: " + be.getMessage()).build();
    } catch (Exception e) {
      return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Error auth").build();
    }
  }

  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws SQLException, IOException {
    try (Connection con = Db.connect();
        PreparedStatement ps = con
            .prepareStatement("SELECT id_obra, id_tipo_obra, titulo, descripcion FROM obras ORDER BY id_obra");
        ResultSet rs = ps.executeQuery()) {
      List<Obra> out = new ArrayList<>();
      while (rs.next()) {
        Obra o = new Obra();
        o.setId_obra(rs.getLong("id_obra"));
        o.setId_tipo_obra(rs.getLong("id_tipo_obra"));
        o.setTitulo(rs.getString("titulo"));
        o.setDescripcion(rs.getString("descripcion"));
        out.add(o);
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id, String includeImage)
      throws SQLException, IOException {
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "SELECT id_obra, id_tipo_obra, titulo, descripcion, imagen FROM obras WHERE id_obra = ?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next())
          return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
        Obra o = new Obra();
        o.setId_obra(rs.getLong("id_obra"));
        o.setId_tipo_obra(rs.getLong("id_tipo_obra"));
        o.setTitulo(rs.getString("titulo"));
        o.setDescripcion(rs.getString("descripcion"));
        if ("true".equalsIgnoreCase(includeImage)) {
          byte[] img = rs.getBytes("imagen");
          if (img != null)
            o.setImagenBase64(Base64.getEncoder().encodeToString(img));
        }
        return json(req, o, HttpStatus.OK);
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Obra in = MAPPER.readValue(req.getBody().orElse("{}"), Obra.class);
    byte[] imageBytes = (in.getImagenBase64() != null) ? Base64.getDecoder().decode(in.getImagenBase64()) : null;

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO obras (id_tipo_obra, titulo, descripcion, imagen) VALUES (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {

      if (in.getId_tipo_obra() != null)
        ps.setLong(1, in.getId_tipo_obra());
      else
        ps.setNull(1, Types.BIGINT);
      ps.setString(2, in.getTitulo());
      ps.setString(3, in.getDescripcion());
      if (imageBytes != null)
        ps.setBytes(4, imageBytes);
      else
        ps.setNull(4, Types.BINARY);
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          long newId = keys.getLong(1);
          return obtener(req, newId, null);
        }
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws Exception {
    Obra in = MAPPER.readValue(req.getBody().orElse("{}"), Obra.class);
    byte[] imageBytes = (in.getImagenBase64() != null) ? Base64.getDecoder().decode(in.getImagenBase64()) : null;

    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "UPDATE obras SET id_tipo_obra = ?, titulo = ?, descripcion = ?, imagen = COALESCE(?, imagen) WHERE id_obra = ?")) {

      if (in.getId_tipo_obra() != null)
        ps.setLong(1, in.getId_tipo_obra());
      else
        ps.setNull(1, Types.BIGINT);
      ps.setString(2, in.getTitulo());
      ps.setString(3, in.getDescripcion());
      if (imageBytes != null)
        ps.setBytes(4, imageBytes);
      else
        ps.setNull(4, Types.BINARY);
      ps.setLong(5, id);
      int rows = ps.executeUpdate();
      if (rows == 0)
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id, null);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id) throws SQLException {
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement("DELETE FROM obras WHERE id_obra = ?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();
      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    }
  }

  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status)
      throws IOException {
    return req.createResponseBuilder(status)
        .header("Content-Type", "application/json")
        .body(MAPPER.writeValueAsString(body))
        .build();
  }
}
