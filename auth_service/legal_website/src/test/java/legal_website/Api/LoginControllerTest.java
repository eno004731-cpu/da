package legal_website.Api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import legal_website.common.errors.OAuht.InvalidGoogleId;
import legal_website.common.errors.OAuht.OAuthConfigurationException;
import legal_website.common.errors.OAuht.OAuthProviderUnavailableException;
import legal_website.common.errors.token.InvalidCredentialsException;
import legal_website.common.errors.token.InvalidFlowTokenException;
import legal_website.Services.auth.login.GoogleFillService;
import legal_website.Services.auth.login.GoogleLoginService;
import legal_website.Services.auth.login.LoginService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoginService loginService;

    @Mock
    private GoogleLoginService googleLoginService;

    @Mock
    private GoogleFillService googleFillService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new LoginController(loginService, googleLoginService, googleFillService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnBadRequestWhenLoginEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bad-email",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenLoginPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@test.ru",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        org.mockito.Mockito.when(loginService.loginUser(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidCredentialsException("не правильный пароль"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@test.ru",
                                  "password": "Wrong123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("не правильный пароль"));
    }

    @Test
    void shouldReturnBadRequestWhenGoogleCredentialIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credential": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenGoogleCompletionPasswordIsWeak() throws Exception {
        mockMvc.perform(post("/api/auth/google/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flowToken": "flow-token",
                                  "fullName": "Е Но",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenGoogleFlowTokenIsInvalid() throws Exception {
        org.mockito.Mockito.when(googleFillService.fillGoogle(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidFlowTokenException("Invalid flow token"));

        mockMvc.perform(post("/api/auth/google/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flowToken": "broken-token",
                                  "fullName": "Е Но",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid flow token"));
    }

    @Test
    void shouldReturnBadRequestWhenGoogleIdTokenIsInvalid() throws Exception {
        org.mockito.Mockito.when(googleLoginService.loginGoogle(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidGoogleId("Google ID token не прошёл verify."));

        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credential": "bad-google-token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Google ID token не прошёл verify."));
    }

    @Test
    void shouldReturnInternalServerErrorWhenGoogleClientIdIsMissing() throws Exception {
        org.mockito.Mockito.when(googleLoginService.loginGoogle(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new OAuthConfigurationException("Не задан google.client-id. Добавь его в application.yaml, .env или переменные окружения."));

        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credential": "google-token"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Не задан google.client-id. Добавь его в application.yaml, .env или переменные окружения."));
    }

    @Test
    void shouldReturnServiceUnavailableWhenGoogleProviderCannotBeValidated() throws Exception {
        org.mockito.Mockito.when(googleLoginService.loginGoogle(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new OAuthProviderUnavailableException("Не удалось провалидировать Google ID token.", new RuntimeException("boom")));

        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credential": "google-token"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Не удалось провалидировать Google ID token."));
    }
}
