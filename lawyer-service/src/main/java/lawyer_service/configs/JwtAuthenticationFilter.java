package lawyer_service.configs;


import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lawyer_service.services.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter{
    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        FilterChain filterChain
    ) throws ServletException, IOException{
        String header = httpServletRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            doFilter(httpServletRequest, httpServletResponse, filterChain);
            return;
        }
        String token = header.substring(7);
        Optional<String> rawEmail = tokenService.getEmailByAcsessToken(token);
        if (rawEmail.isEmpty()) {
            doFilter(httpServletRequest, httpServletResponse, filterChain);
            return;
        }
        String email = rawEmail.get();
        UserDetails user;
        try {
            UserDetails user1 = userDetailsService.loadUserByUsername(email);   
            user=user1;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.warn(
                "Failed to load user from JWT: email={}, method={}, uri={}",
                email,
                httpServletRequest.getMethod(),
                httpServletRequest.getRequestURI(),
                e
            );
            doFilter(httpServletRequest, httpServletResponse, filterChain);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        doFilter(httpServletRequest, httpServletResponse, filterChain);
    }

    private void doFilter(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (ServletException | IOException | RuntimeException e) {
            log.error(
                "Failed to continue filter chain: method={}, uri={}",
                httpServletRequest.getMethod(),
                httpServletRequest.getRequestURI(),
                e
            );
            throw e;
        }
    }
}
