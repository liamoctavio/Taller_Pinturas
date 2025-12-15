package com.function;

import com.function.auth.JwtAuthService;
import com.function.common.HttpConstants;
import com.function.db.Db;
import com.function.events.EventBusEG;
import com.microsoft.azure.functions.*;
import com.nimbusds.jwt.JWTClaimsSet;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventosFunctionTest {

    EventosFunction function;

    @Mock
    ExecutionContext context;

    @Mock
    Logger logger;

    @Mock
    HttpRequestMessage<Optional<String>> request;

    @Mock
    HttpResponseMessage.Builder responseBuilder;

    @BeforeEach
    void setup() {
        function = new EventosFunction();
        when(context.getLogger()).thenReturn(logger);
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.header(anyString(), anyString())).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(mock(HttpResponseMessage.class));
    }

    // ----------------------------------------------------------------
    // eventosRoot
    // ----------------------------------------------------------------

    //comentado temporalmente por el momento ya que no se usa
    // @Test
    // void eventosRoot_sinAuthorization_retorna401() throws Exception {
    //     when(request.getHeaders()).thenReturn(Map.of());
    //     when(request.getHttpMethod()).thenReturn(HttpMethod.GET);

    //     HttpResponseMessage response = function.eventosRoot(request, context);

    //     verify(request).createResponseBuilder(HttpStatus.UNAUTHORIZED);
    //     verify(responseBuilder).body(HttpConstants.ERROR_MISSING_AUTH);
    //     assertNotNull(response);
    // }

    @Test
    void eventosRoot_tokenValido_listarOK() throws Exception {
        when(request.getHeaders()).thenReturn(Map.of("Authorization", "Bearer token"));
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);

        try (
            MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class);
            MockedStatic<Db> db = mockStatic(Db.class)
        ) {
            jwt.when(() -> JwtAuthService.validate(anyString()))
               .thenReturn(new JWTClaimsSet.Builder().subject("svc").build());

            Connection con = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            db.when(Db::connect).thenReturn(con);
            when(con.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            HttpResponseMessage response = function.eventosRoot(request, context);

            verify(request).createResponseBuilder(HttpStatus.OK);
            assertNotNull(response);
        }
    }

    // ----------------------------------------------------------------
    // eventosById - GET
    // ----------------------------------------------------------------

    @Test
    void eventosById_idInvalido_retorna400() throws Exception {
        when(request.getHeaders()).thenReturn(Map.of("Authorization", "Bearer token"));
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);

        try (MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class)) {
            jwt.when(() -> JwtAuthService.validate(anyString()))
               .thenReturn(new JWTClaimsSet.Builder().build());

            HttpResponseMessage response = function.eventosById(request, "abc", context);

            verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
            assertNotNull(response);
        }
    }

    @Test
    void eventosById_noEncontrado_retorna404() throws Exception {
        when(request.getHeaders()).thenReturn(Map.of("Authorization", "Bearer token"));
        when(request.getHttpMethod()).thenReturn(HttpMethod.GET);

        try (
            MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class);
            MockedStatic<Db> db = mockStatic(Db.class)
        ) {
            jwt.when(() -> JwtAuthService.validate(anyString()))
               .thenReturn(new JWTClaimsSet.Builder().build());

            Connection con = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            db.when(Db::connect).thenReturn(con);
            when(con.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            HttpResponseMessage response = function.eventosById(request, "2", context);

            verify(request).createResponseBuilder(HttpStatus.NOT_FOUND);
            assertNotNull(response);
        }
    }

    // ----------------------------------------------------------------
    // eventosById - DELETE
    // ----------------------------------------------------------------

    @Test
    void eliminar_evento_sinAdmin_retorna403() throws Exception {

        when(request.getHeaders()).thenReturn(Map.of(
            "Authorization", "Bearer token"
        ));
        when(request.getHttpMethod()).thenReturn(HttpMethod.DELETE);
        when(request.getQueryParameters()).thenReturn(
            Map.of("id_azure", "5f784b53-452d-438f-a2b3-3772f76f23db")
        );

        try (
            MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class);
            MockedStatic<Db> db = mockStatic(Db.class)
        ) {
            jwt.when(() -> JwtAuthService.validate(anyString()))
            .thenReturn(new JWTClaimsSet.Builder().build());

            Connection con = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            db.when(Db::connect).thenReturn(con);

            // Admin check → NO admin
            when(con.prepareStatement(startsWith("SELECT id_rol"))).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            // Owner check → NO dueño
            when(con.prepareStatement(startsWith("SELECT 1 FROM eventos"))).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            HttpResponseMessage response = function.eventosById(request, "2", context);

            verify(request).createResponseBuilder(HttpStatus.FORBIDDEN);
            assertNotNull(response);
        }
    }


    @Test
    void eliminar_evento_admin_OK() throws Exception {

        when(request.getHeaders()).thenReturn(Map.of(
            "Authorization", "Bearer token"
        ));
        when(request.getHttpMethod()).thenReturn(HttpMethod.DELETE);
        when(request.getQueryParameters()).thenReturn(
            Map.of("id_azure", "5f784b53-452d-438f-a2b3-3772f76f23db")
        );

        try (
            MockedStatic<JwtAuthService> jwt = mockStatic(JwtAuthService.class);
            MockedStatic<Db> db = mockStatic(Db.class)
        ) {
            jwt.when(() -> JwtAuthService.validate(anyString()))
            .thenReturn(new JWTClaimsSet.Builder().build());

            Connection con = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);

            db.when(Db::connect).thenReturn(con);

            // Admin check → SÍ admin
            when(con.prepareStatement(startsWith("SELECT id_rol"))).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("id_rol")).thenReturn(1L);

            // Delete
            when(con.prepareStatement(startsWith("DELETE FROM eventos"))).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            HttpResponseMessage response = function.eventosById(request, "2", context);

            verify(request).createResponseBuilder(HttpStatus.OK);
            assertNotNull(response);
        }
    }

}
