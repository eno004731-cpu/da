package legal_website.servletConfig;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

class JwtAuthenticationEntryPointTest {

    private ObjectMapper objectMapper;
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule javaTimeAsStringModule = new SimpleModule();
        javaTimeAsStringModule.addSerializer(LocalDateTime.class, new StdSerializer<>(LocalDateTime.class) {
            @Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeString(value.toString());
            }
        });
        objectMapper.registerModule(javaTimeAsStringModule);
        jwtAuthenticationEntryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Test
    void shouldReturnInvalidTokenMessageForBadCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Невалидный access token")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(401, body.get("status").asInt());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("Невалидный access token", body.get("message").asText());
        assertEquals("/api/auth/me", body.get("path").asText());
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void shouldReturnDefaultMessageWhenAuthenticationIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Full authentication is required")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(401, response.getStatus());
        assertEquals("Требуется авторизация", body.get("message").asText());
    }
}
