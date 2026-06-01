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
import legal_website.common.errors.User.UserAlreadyExistsException;
import legal_website.Services.auth.register.RegService;

@ExtendWith(MockitoExtension.class)
class RegControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RegService regService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new RegController(regService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnBadRequestWhenRegisterEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ivan Ivanov",
                                  "email": "bad-email",
                                  "phone": "+79990000000",
                                  "companyName": "OOO Test",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRegisterFullNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "ivan@test.ru",
                                  "phone": "+79990000000",
                                  "companyName": "OOO Test",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyExists() throws Exception {
        org.mockito.Mockito.when(regService.regUser(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new UserAlreadyExistsException("уже есть такой пользователь"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ivan Ivanov",
                                  "email": "ivan@test.ru",
                                  "phone": "+79990000000",
                                  "companyName": "OOO Test",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("уже есть такой пользователь"));
    }

    @Test
    void shouldReturnBadRequestWhenRegisterPasswordIsWeak() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ivan Ivanov",
                                  "email": "ivan@test.ru",
                                  "phone": "+79990000000",
                                  "companyName": "OOO Test",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
