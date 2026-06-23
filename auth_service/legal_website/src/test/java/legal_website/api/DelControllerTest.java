package legal_website.api;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import legal_website.services.delete.DelService;
import legal_website.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class DelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DelService delService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DelController(delService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnNoContentWhenDeleteAccountSucceeds() throws Exception {
        try {
            // @AuthenticationPrincipal читает principal из SecurityContext, как после JwtAuthenticationFilter.
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(7L, null));

            mockMvc.perform(delete("/api/auth/account"))
                    .andExpect(status().isNoContent());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(delService).delUser(7L);
    }
}
