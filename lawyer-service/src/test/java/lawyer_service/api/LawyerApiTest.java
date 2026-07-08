package lawyer_service.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lawyer_service.dto.AuthRequest;
import lawyer_service.dto.AuthResponse;
import lawyer_service.dto.LawyerInformation;
import lawyer_service.repo_entity.enums.Role;
import lawyer_service.repo_entity.enums.StatusLawyer;
import lawyer_service.services.RegisterService;

class LawyerApiTest {
    private static final UUID LAWYER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String ACCESS_TOKEN = "access.jwt.token";
    private static final String REFRESH_TOKEN = "refresh.jwt.token";

    // Positive: registration returns public auth body and sends refresh token only as HttpOnly cookie.
    @Test
    void registerShouldReturnCreatedResponseWithAccessTokenAndRefreshCookie() throws Exception {
        RegisterService registerService = mock(RegisterService.class);
        MockMvc mockMvc = mockMvc(registerService);
        AuthRequest request = authRequest();
        when(registerService.reg(any(AuthRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/lawyer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=" + REFRESH_TOKEN)))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
            .andExpect(jsonPath("$.laweyr").value(LAWYER_ID.toString()))
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.refreshToken").value(nullValue()))
            .andExpect(jsonPath("$.role").value(Role.LAWYER.name()))
            .andExpect(jsonPath("$.status").value(StatusLawyer.PENDING_VERIFICATION.name()))
            .andExpect(jsonPath("$.lawyerInformation.email").value(request.getEmail()))
            .andExpect(jsonPath("$.lawyerInformation.firstName").value(request.getFirstName()))
            .andExpect(jsonPath("$.lawyerInformation.lastName").value(request.getLastName()))
            .andExpect(jsonPath("$.lawyerInformation.phone").value(request.getPhone()))
            .andExpect(jsonPath("$.lawyerInformation.specialization").value(request.getSpecialization()));

        verify(registerService).reg(any(AuthRequest.class));
    }

    // Negative: existing email returns conflict and does not set refresh cookie.
    @Test
    void registerShouldReturnConflictWithoutRefreshCookieWhenEmailAlreadyExists() throws Exception {
        RegisterService registerService = mock(RegisterService.class);
        MockMvc mockMvc = mockMvc(registerService);
        AuthRequest request = authRequest();
        when(registerService.reg(any(AuthRequest.class)))
            .thenThrow(new HttpStatusCodeException(HttpStatus.CONFLICT, "Уже есть такой аккаунт") {});

        mockMvc.perform(post("/api/auth/lawyer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(registerService).reg(any(AuthRequest.class));
    }

    private MockMvc mockMvc(RegisterService registerService) {
        return MockMvcBuilders.standaloneSetup(new LawyerApi(registerService))
            .setControllerAdvice(new TestExceptionHandler())
            .build();
    }

    private AuthRequest authRequest() {
        AuthRequest request = new AuthRequest();
        request.setEmail("lawyer@example.com");
        request.setPassword("StrongPassword123!");
        request.setFirstName("Nikita");
        request.setLastName("Ivanov");
        request.setMiddleName("Sergeevich");
        request.setPhone("+79990001122");
        request.setSpecialization("Civil law");
        request.setIp("127.0.0.1");
        request.setAgentId("Chrome/126");
        return request;
    }

    private AuthResponse authResponse() {
        Instant now = Instant.parse("2026-07-08T12:00:00Z");
        AuthResponse response = new AuthResponse();
        response.setLaweyr(LAWYER_ID);
        response.setAccessToken(ACCESS_TOKEN);
        response.setRefreshToken(REFRESH_TOKEN);
        response.setRole(Role.LAWYER);
        response.setStatus(StatusLawyer.PENDING_VERIFICATION);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setLastLoginAt(now);
        response.setLawyerInformation(lawyerInformation());
        return response;
    }

    private LawyerInformation lawyerInformation() {
        LawyerInformation information = new LawyerInformation();
        information.setEmail("lawyer@example.com");
        information.setFirstName("Nikita");
        information.setLastName("Ivanov");
        information.setMiddleName("Sergeevich");
        information.setPhone("+79990001122");
        information.setSpecialization("Civil law");
        return information;
    }

    @RestControllerAdvice
    private static class TestExceptionHandler {
        @ExceptionHandler(HttpStatusCodeException.class)
        ResponseEntity<Void> handleHttpStatusCodeException(HttpStatusCodeException exception) {
            return ResponseEntity.status(exception.getStatusCode()).build();
        }
    }
}
