package com.function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * GraphQL function that delegates to Obras, Eventos and Usuarios APIs via HTTP.
 * Environment variables required:
 *   API_TALLER_PINTURAS     e.g. https://<app>.azurewebsites.net
 *   SERVICE_AUTH_TOKEN (bearer token for service-to-service auth OR "key:<function-key>")
 *
 */
public class FunctionGraphQL {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  static HttpClient httpClient = HttpClient.newHttpClient(); // visible para test

  static void setHttpClient(HttpClient client) {
    httpClient = client;
  }

  private static final GraphQL graphQL;

  static {
    // service token (may be "key:<fn-key>" or a JWT)
    final String SERVICE_AUTH_TOKEN = Optional.ofNullable(System.getenv("SERVICE_AUTH_TOKEN")).orElse("");

    // resolver: if env var is set and not "self" return it; otherwise build from WEBSITE_HOSTNAME
    java.util.function.Function<String,String> resolveBase = (envName) -> {
      String envVal = Optional.ofNullable(System.getenv(envName)).orElse("").trim();
      if (!envVal.isBlank() && !"self".equalsIgnoreCase(envVal)) {
        return envVal;
      }
      String site = System.getenv("WEBSITE_HOSTNAME");
      if (site != null && !site.isBlank()) return "https://" + site;
      return "";
    };

   String base = System.getProperty(
        "API_TALLER_PINTURAS",
        System.getenv("API_TALLER_PINTURAS")
    );

    if (base == null || base.isBlank()) {
        throw new IllegalStateException(
            "Falta configuración de API_TALLER_PINTURAS"
        );
    }
    final String URL_OBRAS = joinUrl(base, "/api/obras");
    final String URL_EVENTOS = joinUrl(base, "/api/eventos");
    final String URL_USUARIOS = joinUrl(base, "/api/usuarios");

    // --- DataFetchers ---

    DataFetcher<List<Map<String,Object>>> obrasListDF = env -> {
      Boolean includeImage = env.getArgument("includeImage");
      String url = URL_OBRAS + (includeImage != null && includeImage ? "?includeImage=true" : "");
      return getJson(url, SERVICE_AUTH_TOKEN, new TypeReference<List<Map<String,Object>>>(){});
    };

    DataFetcher<Map<String,Object>> obraByIdDF = env -> {
      Object idArg = env.getArgument("id");
      if (idArg == null) return null;
      String id = String.valueOf(idArg);
      String url = joinUrl(URL_OBRAS, "/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
      Object inc = env.getArgument("includeImage");
      if (inc instanceof Boolean && (Boolean)inc) url += "?includeImage=true";
      return getJson(url, SERVICE_AUTH_TOKEN, new TypeReference<Map<String,Object>>(){});
    };

    DataFetcher<List<Map<String,Object>>> eventosListDF = env -> {
      return getJson(URL_EVENTOS, SERVICE_AUTH_TOKEN, new TypeReference<List<Map<String,Object>>>(){});
    };

    DataFetcher<Map<String,Object>> eventoByIdDF = env -> {
      Object idArg = env.getArgument("id");
      if (idArg == null) return null;
      String id = String.valueOf(idArg);
      String url = joinUrl(URL_EVENTOS, "/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
      return getJson(url, SERVICE_AUTH_TOKEN, new TypeReference<Map<String,Object>>(){});
    };

    DataFetcher<List<Map<String,Object>>> usuariosListDF = env -> {
      return getJson(URL_USUARIOS, SERVICE_AUTH_TOKEN, new TypeReference<List<Map<String,Object>>>(){});
    };

    DataFetcher<Map<String,Object>> usuarioByIdDF = env -> {
      Object idArg = env.getArgument("id");
      if (idArg == null) return null;
      String id = String.valueOf(idArg);
      String url = joinUrl(URL_USUARIOS, "/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
      return getJson(url, SERVICE_AUTH_TOKEN, new TypeReference<Map<String,Object>>(){});
    };

    DataFetcher<Map<String,Object>> crearObraDF = env -> {
      @SuppressWarnings("unchecked")
      Map<String,Object> input = env.getArgument("input");
      if (input == null) input = Collections.emptyMap();
      return postJson(URL_OBRAS, SERVICE_AUTH_TOKEN, input, new TypeReference<Map<String,Object>>() {});
    };

    // --- GraphQL Types (anidados) ---

    GraphQLObjectType tipoObraType = GraphQLObjectType.newObject()
        .name("TipoObra")
        .field(f -> f.name("id_tipo_obra").type(Scalars.GraphQLInt))
        .field(f -> f.name("nombre").type(Scalars.GraphQLString))
        .build();

    GraphQLObjectType tipoEventoType = GraphQLObjectType.newObject()
        .name("TipoEvento")
        .field(f -> f.name("id_tipo_evento").type(Scalars.GraphQLInt))
        .field(f -> f.name("nombre").type(Scalars.GraphQLString))
        .build();

    GraphQLObjectType rolRefType = GraphQLObjectType.newObject()
        .name("RolRef")
        .field(f -> f.name("id_rol").type(Scalars.GraphQLInt))
        .field(f -> f.name("nombre_rol").type(Scalars.GraphQLString))
        .build();

    GraphQLObjectType usuarioRefType = GraphQLObjectType.newObject()
        .name("UsuarioRef")
        .field(f -> f.name("id_azure").type(Scalars.GraphQLString))
        .field(f -> f.name("username").type(Scalars.GraphQLString))
        .field(f -> f.name("nombre_completo").type(Scalars.GraphQLString))
        .build();

    GraphQLObjectType obraType = GraphQLObjectType.newObject()
        .name("Obra")
        .field(f -> f.name("id_obra").type(Scalars.GraphQLInt))
        .field(f -> f.name("tipo").type(tipoObraType))
        .field(f -> f.name("titulo").type(Scalars.GraphQLString))
        .field(f -> f.name("descripcion").type(Scalars.GraphQLString))
        .field(f -> f.name("imagenBase64").type(Scalars.GraphQLString))
        .build();

    GraphQLObjectType eventoType = GraphQLObjectType.newObject()
        .name("Evento")
        .field(f -> f.name("id_eventos").type(Scalars.GraphQLInt))
        .field(f -> f.name("tipo").type(tipoEventoType))
        .field(f -> f.name("usuario").type(usuarioRefType))
        .field(f -> f.name("rol").type(rolRefType))
        .field(f -> f.name("titulo").type(Scalars.GraphQLString))
        .field(f -> f.name("descripcion").type(Scalars.GraphQLString))
        .field(f -> f.name("fechaInicio").type(Scalars.GraphQLString))
        .field(f -> f.name("fechaTermino").type(Scalars.GraphQLString))
        .field(f -> f.name("precio").type(Scalars.GraphQLFloat))
        .field(f -> f.name("direccion").type(Scalars.GraphQLString))
        .build();

    GraphQLObjectType usuarioType = GraphQLObjectType.newObject()
        .name("Usuario")
        .field(f -> f.name("id_azure").type(Scalars.GraphQLString))
        .field(f -> f.name("id_rol").type(Scalars.GraphQLInt))
        .field(f -> f.name("rol").type(rolRefType))
        .field(f -> f.name("id_obra").type(Scalars.GraphQLInt))
        .field(f -> f.name("username").type(Scalars.GraphQLString))
        .field(f -> f.name("nombre_completo").type(Scalars.GraphQLString))
        .build();

    // Input type for crearObra
    GraphQLInputObjectType obraInput = GraphQLInputObjectType.newInputObject()
        .name("ObraInput")
        .field(GraphQLInputObjectField.newInputObjectField().name("id_tipo_obra").type(Scalars.GraphQLInt).build())
        .field(GraphQLInputObjectField.newInputObjectField().name("titulo").type(Scalars.GraphQLString).build())
        .field(GraphQLInputObjectField.newInputObjectField().name("descripcion").type(Scalars.GraphQLString).build())
        .field(GraphQLInputObjectField.newInputObjectField().name("imagenBase64").type(Scalars.GraphQLString).build())
        .build();

    // --- Query type ---
    GraphQLObjectType queryType = GraphQLObjectType.newObject()
        .name("Query")
        .field(f -> f.name("obras")
            .type(new GraphQLList(obraType))
            .argument(a -> a.name("includeImage").type(Scalars.GraphQLBoolean))
            .dataFetcher(obrasListDF))
        .field(f -> f.name("obra")
            .type(obraType)
            .argument(a -> a.name("id").type(Scalars.GraphQLInt))
            .argument(a -> a.name("includeImage").type(Scalars.GraphQLBoolean))
            .dataFetcher(obraByIdDF))
        .field(f -> f.name("eventos")
            .type(new GraphQLList(eventoType))
            .dataFetcher(eventosListDF))
        .field(f -> f.name("evento")
            .type(eventoType)
            .argument(a -> a.name("id").type(Scalars.GraphQLInt))
            .dataFetcher(eventoByIdDF))
        .field(f -> f.name("usuarios")
            .type(new GraphQLList(usuarioType))
            .dataFetcher(usuariosListDF))
        .field(f -> f.name("usuario")
            .type(usuarioType)
            .argument(a -> a.name("id").type(Scalars.GraphQLString))
            .dataFetcher(usuarioByIdDF))
        .build();

    // --- Mutation type ---
    GraphQLObjectType mutationType = GraphQLObjectType.newObject()
        .name("Mutation")
        .field(f -> f.name("crearObra")
            .type(obraType)
            .argument(a -> a.name("input").type(obraInput))
            .dataFetcher(crearObraDF))
        .build();

    GraphQLSchema schema = GraphQLSchema.newSchema()
        .query(queryType)
        .mutation(mutationType)
        .build();

    graphQL = GraphQL.newGraphQL(schema).build();
  }

  // --- Helpers (HTTP + JSON) ---

  private static String joinUrl(String base, String path) {
    String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    String p = path.startsWith("/") ? path : ("/" + path);
    return b + p;
  }

  private static HttpRequest.Builder getBuilder(String url, String serviceToken) {
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json")
        .GET();
    if (serviceToken != null && !serviceToken.isBlank()) {
      if (serviceToken.startsWith("key:")) {
        b.header("x-functions-key", serviceToken.substring(4));
      } else {
        b.header("Authorization", "Bearer " + serviceToken);
      }
    }
    return b;
  }

  private static HttpRequest.Builder postBuilder(String url, String serviceToken, String json) {
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json));
    if (serviceToken != null && !serviceToken.isBlank()) {
      if (serviceToken.startsWith("key:")) {
        b.header("x-functions-key", serviceToken.substring(4));
      } else {
        b.header("Authorization", "Bearer " + serviceToken);
      }
    }
    return b;
  }

  private static <T> T getJson(String url, String serviceToken, TypeReference<T> type) throws Exception {
    HttpResponse<String> resp = httpClient.send(getBuilder(url, serviceToken).build(), HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() / 100 == 2) {
      String body = resp.body();
      if (body == null || body.isBlank()) {
        if (type.getType().getTypeName().startsWith("java.util.List")) return (T) Collections.emptyList();
        return null;
      }
      return MAPPER.readValue(body, type);
    }
    throw new RuntimeException("GET " + url + " -> " + resp.statusCode() + " " + resp.body());
  }

  private static <T> T postJson(String url, String serviceToken, Object body, TypeReference<T> type) throws Exception {
    String json = (body instanceof String) ? (String) body : MAPPER.writeValueAsString(body);
    HttpResponse<String> resp = httpClient.send(postBuilder(url, serviceToken, json).build(), HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() / 100 == 2 || resp.statusCode() == 201) {
      String b = resp.body();
      if (b == null || b.isBlank()) return null;
      return MAPPER.readValue(b, type);
    }
    throw new RuntimeException("POST " + url + " -> " + resp.statusCode() + " " + resp.body());
  }

  @FunctionName("graphql")
  public HttpResponseMessage run(
      @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, route = "graphql")
      HttpRequestMessage<Map<String, Object>> request,
      final ExecutionContext context) {

    Map<String, Object> body = request.getBody();
    if (body == null || !body.containsKey("query")) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("Content-Type","application/json")
          .body(Map.of("error","Body JSON inválido. Esperado: { \"query\": \"...\" }"))
          .build();
    }

    String query = String.valueOf(body.get("query"));
    Map<String,Object> variables = Map.of();
    Object vars = body.get("variables");
    if (vars instanceof Map) variables = (Map<String,Object>) vars;

    ExecutionInput input = ExecutionInput.newExecutionInput()
        .query(query)
        .variables(variables)
        .build();

    Map<String,Object> result = graphQL.execute(input).toSpecification();
    return request.createResponseBuilder(HttpStatus.OK)
        .header("Content-Type","application/json")
        .body(result)
        .build();
  }
}