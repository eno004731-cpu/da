package legal_website.Services.auth.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import legal_website.Dto.login.LoginRequest;
import legal_website.Dto.register.AuthResponse;
import legal_website.Dto.register.AuthUserResponse;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.Services.auth.AuthSessionService;
import legal_website.common.errors.token.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private LoginService loginService;

    @Test
    void loginUserShouldReturnJwtContractAndSaveRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@test.ru");
        request.setPassword("secret");

        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setEmail("ivan@test.ru");
        user.setPasswordHash("hashed-password");
        user.setFullName("Ivan Ivanov");
        user.setPhone("+79990000000");
        user.setCompanyName("OOO Test");
        user.setRole("CLIENT");
        user.setActive(true);

        AuthUserResponse userResponse = new AuthUserResponse();
        userResponse.setId(7L);
        userResponse.setEmail("ivan@test.ru");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("access-token");
        authResponse.setRefreshToken("raw-refresh-token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(900L);
        authResponse.setUser(userResponse);

        when(userRepo.findByEmail("ivan@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed-password")).thenReturn(true);
        when(authSessionService.buildUserResponse(user)).thenReturn(userResponse);
        when(authSessionService.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(authSessionService.buildAuthResponse(user, "raw-refresh-token", userResponse))
                .thenReturn(authResponse);

        AuthResponse response = loginService.loginUser(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("raw-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertEquals(7L, response.getUser().getId());
        assertEquals("ivan@test.ru", response.getUser().getEmail());
        verify(authSessionService).saveToken(user, "raw-refresh-token");
    }

    @Test
    void loginUserShouldThrowWhenPasswordIsWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@test.ru");
        request.setPassword("wrong");

        UserEntity user = new UserEntity();
        user.setEmail("ivan@test.ru");
        user.setPasswordHash("hashed-password");
        user.setActive(true);

        when(userRepo.findByEmail("ivan@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        InvalidCredentialsException error = assertThrows(InvalidCredentialsException.class, () -> loginService.loginUser(request));
        assertEquals("не правильный пароль", error.getMessage());
        verify(authSessionService, never()).saveToken(any(), any());
    }
}
