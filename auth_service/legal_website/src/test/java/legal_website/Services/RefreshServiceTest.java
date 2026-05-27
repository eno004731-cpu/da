package legal_website.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import legal_website.Dto.refreshToken.TokenRequest;
import legal_website.Dto.register.AuthResponse;
import legal_website.Dto.register.AuthUserResponse;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Jwt.JwtEntity;
import legal_website.EntityAndRepo.Jwt.JwtRepo;
import legal_website.Services.Jwt.JwtService;
import legal_website.Services.auth.AuthSessionService;
import legal_website.Services.auth.RefreshService;
import legal_website.common.errors.InactiveUserException;
import legal_website.common.errors.RefreshTokenNotFoundException;
import legal_website.common.errors.RefreshTokenRevokedException;
import legal_website.common.errors.TokenValidationException;

@ExtendWith(MockitoExtension.class)
class RefreshServiceTest {

    @Mock
    private JwtRepo jwtRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private RefreshService refreshService;

    @Test
    void refreshTokenShouldRevokeOldTokenSaveNewTokenAndReturnJwtContract() {
        TokenRequest request = new TokenRequest();
        request.setRefreshToken("old-refresh-token");

        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", 11L);
        user.setFullName("Ivan Ivanov");
        user.setEmail("ivan@test.ru");
        user.setPhone("+79990000000");
        user.setCompanyName("OOO Test");
        user.setRole("CLIENT");
        user.setActive(true);

        JwtEntity currentToken = new JwtEntity();
        currentToken.setUser(user);
        currentToken.setTokenHash("old-hash");
        currentToken.setCreatedAt(LocalDateTime.now().minusDays(1));
        currentToken.setExpiresAt(LocalDateTime.now().plusDays(10));

        AuthUserResponse userResponse = new AuthUserResponse();
        userResponse.setId(11L);
        userResponse.setEmail("ivan@test.ru");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("new-access-token");
        authResponse.setRefreshToken("new-refresh-token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(900L);
        authResponse.setUser(userResponse);

        when(jwtService.getValidRefreshToken("old-refresh-token")).thenReturn(currentToken);
        when(authSessionService.generateRefreshToken()).thenReturn("new-refresh-token");
        when(authSessionService.buildUserResponse(user)).thenReturn(userResponse);
        when(authSessionService.buildAuthResponse(user, "new-refresh-token", userResponse))
                .thenReturn(authResponse);
        when(jwtRepo.save(any(JwtEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = refreshService.refreshToken(request);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertEquals(11L, response.getUser().getId());
        assertEquals("ivan@test.ru", response.getUser().getEmail());

        ArgumentCaptor<JwtEntity> jwtCaptor = ArgumentCaptor.forClass(JwtEntity.class);
        verify(jwtRepo).save(jwtCaptor.capture());

        JwtEntity revokedToken = jwtCaptor.getValue();
        assertEquals(currentToken, revokedToken);
        assertNotNull(revokedToken.getRevokedAt());
        verify(authSessionService).saveToken(user, "new-refresh-token");
    }

    @Test
    void refreshTokenShouldThrowWhenTokenAlreadyRevoked() {
        TokenRequest request = new TokenRequest();
        request.setRefreshToken("revoked-token");

        when(jwtService.getValidRefreshToken("revoked-token"))
                .thenThrow(new RefreshTokenRevokedException("Refresh token уже отозван"));

        RefreshTokenRevokedException error = assertThrows(
                RefreshTokenRevokedException.class,
                () -> refreshService.refreshToken(request)
        );

        assertEquals("Refresh token уже отозван", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
    }

    @Test
    void refreshTokenShouldThrowWhenTokenExpired() {
        TokenRequest request = new TokenRequest();
        request.setRefreshToken("expired-token");

        when(jwtService.getValidRefreshToken("expired-token"))
                .thenThrow(new TokenValidationException("Refresh token истёк"));

        TokenValidationException error = assertThrows(TokenValidationException.class, () -> refreshService.refreshToken(request));

        assertEquals("Refresh token истёк", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
    }

    @Test
    void refreshTokenShouldThrowWhenUserIsInactive() {
        TokenRequest request = new TokenRequest();
        request.setRefreshToken("inactive-user-token");

        UserEntity user = new UserEntity();
        user.setActive(false);

        JwtEntity currentToken = new JwtEntity();
        currentToken.setUser(user);
        currentToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(jwtService.getValidRefreshToken("inactive-user-token")).thenReturn(currentToken);

        InactiveUserException error = assertThrows(
                InactiveUserException.class,
                () -> refreshService.refreshToken(request)
        );

        assertEquals("не активный пользователь", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
        verify(authSessionService, never()).saveToken(any(), any());
    }

    @Test
    void refreshTokenShouldThrowWhenTokenDoesNotExist() {
        TokenRequest request = new TokenRequest();
        request.setRefreshToken("missing-token");

        when(jwtService.getValidRefreshToken("missing-token"))
                .thenThrow(new RefreshTokenNotFoundException("нет токена"));

        RefreshTokenNotFoundException error = assertThrows(
                RefreshTokenNotFoundException.class,
                () -> refreshService.refreshToken(request)
        );

        assertEquals("нет токена", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
        verify(authSessionService, never()).saveToken(any(), any());
    }
}
