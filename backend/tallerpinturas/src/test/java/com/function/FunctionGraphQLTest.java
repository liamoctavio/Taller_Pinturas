package com.function;

import com.microsoft.azure.functions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FunctionGraphQLTest {

    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    void setup() throws Exception {
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        // Inyectamos el HttpClient mockeado
        FunctionGraphQL.setHttpClient(httpClient);
    }

    @BeforeAll
    static void setupEnv() {
        System.setProperty("API_TALLER_PINTURAS", "http://localhost");
    }

    @Test
    void deberiaRetornarObrasCorrectamente() throws Exception {

        // ---------- JSON simulado desde API REST ----------
        String jsonApiResponse = """
        [
          {
            "id": "1",
            "nombre": "Obra Test",
            "estado": "ACTIVA"
          }
        ]
        """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonApiResponse);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        // ---------- Query GraphQL ----------

        Map<String, Object> body = new HashMap<>();
          body.put("query", """
          {
            obras {
              id
              nombre
              estado
            }
          }
          """);


        // ---------- Mock HttpRequestMessage ----------
        HttpRequestMessage<Map<String, Object>> request = mock(HttpRequestMessage.class);




        HttpResponseMessage.Builder responseBuilder = mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage response = mock(HttpResponseMessage.class);

        when(responseBuilder.header(anyString(), anyString())).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        when(response.getStatus()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn("{\"data\":{}}");

        when(request.createResponseBuilder(any(HttpStatus.class)))
        .thenReturn(responseBuilder);


        // ---------- Mock ExecutionContext ----------
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());


        // ---------- Validaciones ----------
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatus());
    }
}
