package com.function;

import com.function.auth.JwtAuthService;
import com.function.db.Db;
import com.microsoft.azure.functions.*;
import com.nimbusds.jwt.JWTClaimsSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObrasFunctionTest {

    private ObrasFunction function;
    private ExecutionContext context;

    @BeforeEach
    void setup() {
        function = new ObrasFunction();
        context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
    }

    /* =========================================================
       Helper para mockear HttpRequestMessage correctamente
       ========================================================= */
    @SuppressWarnings("unchecked")
    private HttpRequestMessage<Optional<String>> mockRequest(
            HttpMethod method,
            String body,
            Map<String, String> headers) {

        HttpRequestMessage<Optional<String>> req =
                (HttpRequestMessage<Optional<String>>) mock(HttpRequestMessage.class);

        when(req.getHttpMethod()).thenReturn(method);
        when(req.getBody()).thenReturn(Optional.ofNullable(body));
        when(req.getHeaders()).thenReturn(headers);
        when(req.getQueryParameters()).thenReturn(new HashMap<>());

        HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(HttpResponseMessage.class));

        when(req.createResponseBuilder(any(HttpStatus.class))).thenReturn(builder);

        return req;
    }

    /* =========================================================
       TESTS obrasRoot
       ========================================================= */

    @Test
    void obrasRoot_methodNotAllowed_devuelve405() throws Exception {

        HttpRequestMessage<Optional<String>> req =
                mockRequest(
                        HttpMethod.PUT,
                        null,
                        Map.of("Authorization", "Bearer test")
                );
        when(req.getHeaders()).thenReturn(Map.of());
        try (MockedStatic<JwtAuthService> jwtMock = mockStatic(JwtAuthService.class)) {

            jwtMock.when(() -> JwtAuthService.validate(anyString()))
                   .thenReturn(mock(JWTClaimsSet.class));

            HttpResponseMessage response = function.obrasRoot(req, context);

            verify(req).createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    /* =========================================================
       TESTS obrasById
       ========================================================= */

    @Test
    void obrasById_idInvalido_devuelve400() throws Exception {

        HttpRequestMessage<Optional<String>> req =
                mockRequest(
                        HttpMethod.GET,
                        null,
                        Map.of("Authorization", "Bearer test")
                );

        when(req.getHeaders()).thenReturn(Map.of());

        try (MockedStatic<JwtAuthService> jwtMock = mockStatic(JwtAuthService.class)) {

            jwtMock.when(() -> JwtAuthService.validate(anyString()))
                   .thenReturn(mock(JWTClaimsSet.class));

            HttpResponseMessage response =
                    function.obrasById(req, "abc", context);

            verify(req).createResponseBuilder(HttpStatus.BAD_REQUEST);
        }
    }

        @Test
        void eliminar_sinRolAdmin_devuelve403() throws Exception {

        HttpRequestMessage<Optional<String>> req =
                mockRequest(
                        HttpMethod.DELETE,
                        null,
                        Map.of("Authorization", "Bearer test")
                );

        when(req.getQueryParameters()).thenReturn(
                Map.of("id_azure", "5f784b53-452d-438f-a2b3-3772f76f23db")
        );

                try (
                        MockedStatic<JwtAuthService> jwtMock = mockStatic(JwtAuthService.class);
                        MockedStatic<Db> dbMock = mockStatic(Db.class)
                ) {
                        jwtMock.when(() -> JwtAuthService.validate(anyString()))
                        .thenReturn(mock(JWTClaimsSet.class));

                        // Mock DB
                        Connection con = mock(Connection.class);
                        PreparedStatement psAdmin = mock(PreparedStatement.class);
                        ResultSet rsAdmin = mock(ResultSet.class);

                        dbMock.when(Db::connect).thenReturn(con);

                        // Admin check → NO es admin
                        when(con.prepareStatement(startsWith("SELECT id_rol"))).thenReturn(psAdmin);
                        when(psAdmin.executeQuery()).thenReturn(rsAdmin);
                        when(rsAdmin.next()).thenReturn(false);

                        // Dueño check → NO es dueño
                        PreparedStatement psOwner = mock(PreparedStatement.class);
                        ResultSet rsOwner = mock(ResultSet.class);
                        when(con.prepareStatement(startsWith("SELECT 1 FROM usuarios_obras")))
                                .thenReturn(psOwner);
                        when(psOwner.executeQuery()).thenReturn(rsOwner);
                        when(rsOwner.next()).thenReturn(false);

                        HttpResponseMessage response =
                                function.obrasById(req, "2", context);

                        verify(req).createResponseBuilder(HttpStatus.FORBIDDEN);
                        assertNotNull(response);
                }
        }


    @Test
    void eliminar_conRolAdmin_intentaEliminar() throws Exception {

        HttpRequestMessage<Optional<String>> req =
                mockRequest(
                        HttpMethod.DELETE,
                        null,
                        Map.of(
                                "Authorization", "Bearer test",
                                "X-User-Roles", "admin"
                        )
                );
        when(req.getHeaders()).thenReturn(Map.of());
        try (MockedStatic<JwtAuthService> jwtMock = mockStatic(JwtAuthService.class)) {

            jwtMock.when(() -> JwtAuthService.validate(anyString()))
                   .thenReturn(mock(JWTClaimsSet.class));

            HttpResponseMessage response =
                    function.obrasById(req, "1", context);

            verify(req).createResponseBuilder(any(HttpStatus.class));
        }
    }
}
