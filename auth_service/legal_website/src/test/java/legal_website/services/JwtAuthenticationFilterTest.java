package legal_website.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;
import legal_website.persistence.auth.UserEntity;
import legal_website.services.jwt.JwtService;
import legal_website.config.JwtAuthenticationFilter;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassRequestWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).getUserFromValidAccessToken(any());
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldPassRequestWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic dXNlcjp0ZXN0");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).getUserFromValidAccessToken(any());
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAuthenticateWhenAccessTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");

        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setEmail("ivan@test.ru");
        user.setRole("CLIENT");

        when(jwtService.getUserFromValidAccessToken("valid-token")).thenReturn(user);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertEquals("7", authentication.getName());
        assertEquals("ROLE_CLIENT", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer broken-token");

        when(jwtService.getUserFromValidAccessToken("broken-token"))
                .thenThrow(new IllegalArgumentException("bad token"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<AuthenticationException> exceptionCaptor =
                ArgumentCaptor.forClass(AuthenticationException.class);

        verify(filterChain, never()).doFilter(request, response);
        verify(authenticationEntryPoint).commence(
                any(),
                any(),
                exceptionCaptor.capture()
        );
        assertEquals("Невалидный access token", exceptionCaptor.getValue().getMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotConvertDownstreamRuntimeExceptionIntoUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");

        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setEmail("ivan@test.ru");
        user.setRole("CLIENT");

        when(jwtService.getUserFromValidAccessToken("valid-token")).thenReturn(user);
        org.mockito.Mockito.doThrow(new RuntimeException("controller boom"))
                .when(filterChain)
                .doFilter(request, response);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> jwtAuthenticationFilter.doFilter(request, response, filterChain)
        );

        assertEquals("controller boom", error.getMessage());
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }
}
