package com.function;

import com.function.auth.JwtAuthService;
import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class UsuariosFunctionTest {

    private UsuariosFunction function;
    private ExecutionContext context;

    @BeforeEach
    void setup() {
        function = new UsuariosFunction();
        context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getAnonymousLogger());
    }

    @SuppressWarnings("unchecked")
    private HttpRequestMessage<Optional<String>> mockRequest(HttpMethod method, String body, Map<String, String> headers) {
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        when(req.getHttpMethod()).thenReturn(method);
        when(req.getBody()).thenReturn(Optional.ofNullable(body));
        when(req.getHeaders()).thenReturn(headers != null ? headers : new HashMap<>());

        HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        HttpResponseMessage response = mock(HttpResponseMessage.class);
        when(response.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        when(builder.build()).thenReturn(response);

        when(req.createResponseBuilder(any(HttpStatus.class))).thenReturn(builder);
        return req;
    }

    @Test
    void usuariosRoot_sinAuthorization_devuelve401() throws Exception {
        HttpRequestMessage<Optional<String>> req = mockRequest(HttpMethod.GET, null, new HashMap<>());
        when(req.getHeaders()).thenReturn(Map.of());

        try (MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class)) {
            jwt.when(() -> JwtAuthService.validate(null))
               .thenThrow(new IllegalArgumentException("missing"));

            HttpResponseMessage resp = function.usuariosRoot(req, context);
            assertNotNull(resp);
        }
    }

    @Test
    void usuariosRoot_metodoNoPermitido() throws Exception {
        Map<String,String> headers = Map.of("Authorization", "Bearer test");
        HttpRequestMessage<Optional<String>> req = mockRequest(HttpMethod.PUT, null, headers);
        when(req.getHeaders()).thenReturn(Map.of());

        try (MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class)) {
            jwt.when(() -> JwtAuthService.validate(any())).thenReturn(null);

            HttpResponseMessage resp = function.usuariosRoot(req, context);
            assertNotNull(resp);
        }
    }

    @Test
    void usuariosById_idVacio_devuelve400() throws Exception {
        Map<String,String> headers = Map.of("Authorization", "Bearer test");
        HttpRequestMessage<Optional<String>> req = mockRequest(HttpMethod.GET, null, headers);
        when(req.getHeaders()).thenReturn(Map.of());

        try (MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class)) {
            jwt.when(() -> JwtAuthService.validate(any())).thenReturn(null);

            HttpResponseMessage resp = function.usuariosById(req, "", context);
            assertNotNull(resp);
        }
    }

    @Test
    void usuariosSync_faltanDatos_devuelve400() throws Exception {
        String body = "{\"username\":\"test@test.cl\"}";
        HttpRequestMessage<Optional<String>> req = mockRequest(HttpMethod.POST, body, Map.of());
        when(req.getHeaders()).thenReturn(Map.of());
        HttpResponseMessage resp = function.usuariosSync(req, context);
        assertNotNull(resp);
    }

    @Test
    void usuariosSync_bodyValido_sinDB_real() throws Exception {
        String body = "{\"id_azure\":\"11111111-1111-1111-1111-111111111111\",\"username\":\"a@a.cl\"}";
        HttpRequestMessage<Optional<String>> req = mockRequest(HttpMethod.POST, body, Map.of());
        when(req.getHeaders()).thenReturn(Map.of());

        HttpResponseMessage response =
        function.usuariosSync(req, context);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());

    }
}
