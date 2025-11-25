package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.auth.JwtAuthService;
import com.function.db.Db;
import com.function.model.Evento;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventosFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("eventos")
  public HttpResponseMessage handle(
      @HttpTrigger(name = "req", methods = { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
          HttpMethod.DELETE }, authLevel = AuthorizationLevel.FUNCTION, route = "eventos/{id?}") HttpRequestMessage<Optional<String>> request,
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
          return (id == null) ? listar(request) : obtener(request, Long.parseLong(id));
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
        PreparedStatement ps = con.prepareStatement(
            "SELECT id_eventos, id_tipo_evento, id_azure, id_rol, titulo, descripcion, fechaInicio, fechaTermino, precio, direccion FROM eventos ORDER BY fechaInicio DESC");
        ResultSet rs = ps.executeQuery()) {
      List<Evento> out = new ArrayList<>();
      while (rs.next()) {
        Evento e = new Evento();
        e.setId_eventos(rs.getLong("id_eventos"));
        e.setId_tipo_evento(rs.getLong("id_tipo_evento"));
        e.setId_azure(rs.getString("id_azure"));
        e.setId_rol(rs.getLong("id_rol"));
        e.setTitulo(rs.getString("titulo"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setFechaInicio(rs.getString("fechaInicio"));
        e.setFechaTermino(rs.getString("fechaTermino"));
        e.setPrecio(rs.getDouble("precio"));
        e.setDireccion(rs.getString("direccion"));
        out.add(e);
      }
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id) throws SQLException, IOException {
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "SELECT id_eventos, id_tipo_evento, id_azure, id_rol, titulo, descripcion, fechaInicio, fechaTermino, precio, direccion FROM eventos WHERE id_eventos = ?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next())
          return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
        Evento e = new Evento();
        e.setId_eventos(rs.getLong("id_eventos"));
        e.setId_tipo_evento(rs.getLong("id_tipo_evento"));
        e.setId_azure(rs.getString("id_azure"));
        e.setId_rol(rs.getLong("id_rol"));
        e.setTitulo(rs.getString("titulo"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setFechaInicio(rs.getString("fechaInicio"));
        e.setFechaTermino(rs.getString("fechaTermino"));
        e.setPrecio(rs.getDouble("precio"));
        e.setDireccion(rs.getString("direccion"));
        return json(req, e, HttpStatus.OK);
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Evento in = MAPPER.readValue(req.getBody().orElse("{}"), Evento.class);
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO eventos (id_tipo_evento, id_azure, id_rol, titulo, descripcion, fechaInicio, fechaTermino, precio, direccion) VALUES (?,?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {

      if (in.getId_tipo_evento() != null)
        ps.setLong(1, in.getId_tipo_evento());
      else
        ps.setNull(1, Types.BIGINT);
      ps.setString(2, in.getId_azure());
      if (in.getId_rol() != null)
        ps.setLong(3, in.getId_rol());
      else
        ps.setNull(3, Types.BIGINT);
      ps.setString(4, in.getTitulo());
      ps.setString(5, in.getDescripcion());
      ps.setString(6, in.getFechaInicio());
      ps.setString(7, in.getFechaTermino());
      if (in.getPrecio() != null)
        ps.setDouble(8, in.getPrecio());
      else
        ps.setNull(8, Types.NUMERIC);
      ps.setString(9, in.getDireccion());
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
    Evento in = MAPPER.readValue(req.getBody().orElse("{}"), Evento.class);
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement(
            "UPDATE eventos SET id_tipo_evento=?, id_azure=?, id_rol=?, titulo=?, descripcion=?, fechaInicio=?, fechaTermino=?, precio=?, direccion=? WHERE id_eventos=?")) {

      if (in.getId_tipo_evento() != null)
        ps.setLong(1, in.getId_tipo_evento());
      else
        ps.setNull(1, Types.BIGINT);
      ps.setString(2, in.getId_azure());
      if (in.getId_rol() != null)
        ps.setLong(3, in.getId_rol());
      else
        ps.setNull(3, Types.BIGINT);
      ps.setString(4, in.getTitulo());
      ps.setString(5, in.getDescripcion());
      ps.setString(6, in.getFechaInicio());
      ps.setString(7, in.getFechaTermino());
      if (in.getPrecio() != null)
        ps.setDouble(8, in.getPrecio());
      else
        ps.setNull(8, Types.NUMERIC);
      ps.setString(9, in.getDireccion());
      ps.setLong(10, id);

      int rows = ps.executeUpdate();
      if (rows == 0)
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id) throws SQLException {
    try (Connection con = Db.connect();
        PreparedStatement ps = con.prepareStatement("DELETE FROM eventos WHERE id_eventos = ?")) {
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
