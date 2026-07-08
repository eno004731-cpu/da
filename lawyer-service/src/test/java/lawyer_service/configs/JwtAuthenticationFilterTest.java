package lawyer_service.configs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.servlet.FilterChain;
import lawyer_service.services.TokenService;

class JwtAuthenticationFilterTest {
    private static final String TOKEN = "valid.jwt.token";
    private static final String EMAIL = "lawyer@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        FilterContext context = filterContext();

        context.filter().doFilter(context.request(), context.response(), context.filterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(context.filterChain()).doFilter(context.request(), context.response());
        verifyNoInteractions(context.tokenService(), context.userDetailsService());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsInvalid() throws Exception {
        FilterContext context = filterContext();
        context.withBearerToken(TOKEN);
        when(context.tokenService().getEmailByAcsessToken(TOKEN)).thenReturn(Optional.empty());

        context.filter().doFilter(context.request(), context.response(), context.filterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(context.tokenService()).getEmailByAcsessToken(TOKEN);
        verifyNoInteractions(context.userDetailsService());
        verify(context.filterChain()).doFilter(context.request(), context.response());
    }

    @Test
    void shouldAuthenticateRequestWhenTokenAndUserAreValid() throws Exception {
        FilterContext context = filterContext();
        UserDetails userDetails = userDetails();
        context.withBearerToken(TOKEN);
        when(context.tokenService().getEmailByAcsessToken(TOKEN)).thenReturn(Optional.of(EMAIL));
        when(context.userDetailsService().loadUserByUsername(EMAIL)).thenReturn(userDetails);

        context.filter().doFilter(context.request(), context.response(), context.filterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        assertThat(authentication.getCredentials()).isNull();

        UserDetails authenticatedUser = (UserDetails) authentication.getPrincipal();
        assertThat(authenticatedUser.getUsername()).isEqualTo(EMAIL);
        assertThat(authenticatedUser.getPassword()).isNull();
        assertThat(authenticatedUser.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_LAWYER");

        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_LAWYER");

        verify(context.tokenService()).getEmailByAcsessToken(TOKEN);
        verify(context.userDetailsService()).loadUserByUsername(EMAIL);
        verify(context.filterChain()).doFilter(context.request(), context.response());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenUserFromTokenIsNotFound() throws Exception {
        FilterContext context = filterContext();
        context.withBearerToken(TOKEN);
        when(context.tokenService().getEmailByAcsessToken(TOKEN)).thenReturn(Optional.of(EMAIL));
        when(context.userDetailsService().loadUserByUsername(EMAIL))
            .thenThrow(new UsernameNotFoundException("User not found"));

        context.filter().doFilter(context.request(), context.response(), context.filterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(context.tokenService()).getEmailByAcsessToken(TOKEN);
        verify(context.userDetailsService()).loadUserByUsername(EMAIL);
        verify(context.filterChain()).doFilter(context.request(), context.response());
    }

    private FilterContext filterContext() {
        TokenService tokenService = mock(TokenService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, userDetailsService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/lawyers/me");

        return new FilterContext(
            filter,
            tokenService,
            userDetailsService,
            request,
            new MockHttpServletResponse(),
            mock(FilterChain.class)
        );
    }

    private UserDetails userDetails() {
        return new TestUserDetails(EMAIL);
    }

    private record FilterContext(
        JwtAuthenticationFilter filter,
        TokenService tokenService,
        CustomUserDetailsService userDetailsService,
        MockHttpServletRequest request,
        MockHttpServletResponse response,
        FilterChain filterChain
    ) {
        private void withBearerToken(String token) {
            request.addHeader("Authorization", "Bearer " + token);
        }
    }

    private record TestUserDetails(String username) implements UserDetails {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(() -> "ROLE_LAWYER");
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }
}
