package legal_website.Api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import legal_website.common.GlobalExceptionHandler;
import legal_website.common.errors.RefreshTokenNotFoundException;
import legal_website.common.errors.RefreshTokenRevokedException;
import legal_website.common.errors.TokenValidationException;
import legal_website.Dto.logout.LogoutRequest;
import legal_website.Services.auth.login.LogoutService;

@ExtendWith(MockitoExtension.class)
class LogoutControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new LogoutController(logoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnBadRequestWhenRefreshTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRefreshTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOkWhenLogoutRequestIsValid() throws Exception {
        when(logoutService.logout(org.mockito.ArgumentMatchers.any(LogoutRequest.class))).thenReturn(true);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldReturnUnauthorizedWhenLogoutTokenIsInvalid() throws Exception {
        when(logoutService.logout(org.mockito.ArgumentMatchers.any(LogoutRequest.class)))
                .thenThrow(new RefreshTokenRevokedException("Refresh token уже отозван"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "revoked-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token уже отозван"));
    }

    @Test
    void shouldReturnNotFoundWhenLogoutTokenDoesNotExist() throws Exception {
        when(logoutService.logout(org.mockito.ArgumentMatchers.any(LogoutRequest.class)))
                .thenThrow(new RefreshTokenNotFoundException("нет токена"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "missing-token"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("нет токена"));
    }
}
