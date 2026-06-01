package legal_website.Services.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import legal_website.Dto.logout.LogoutRequest;
import legal_website.EntityAndRepo.Jwt.JwtEntity;
import legal_website.EntityAndRepo.Jwt.JwtRepo;
import legal_website.Services.Jwt.JwtService;
import legal_website.Services.auth.login.LogoutService;
import legal_website.common.errors.token.RefreshTokenNotFoundException;
import legal_website.common.errors.token.RefreshTokenRevokedException;
import legal_website.common.errors.token.TokenValidationException;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtRepo jwtRepo;

    @InjectMocks
    private LogoutService logoutService;

    @Test
    void logoutShouldRevokeTokenAndSaveIt() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("raw-refresh-token");

        JwtEntity jwtEntity = new JwtEntity();
        jwtEntity.setTokenHash("token-hash");

        when(jwtService.getValidRefreshToken("raw-refresh-token")).thenReturn(jwtEntity);
        when(jwtRepo.save(any(JwtEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = logoutService.logout(request);

        assertTrue(result);
        ArgumentCaptor<JwtEntity> captor = ArgumentCaptor.forClass(JwtEntity.class);
        verify(jwtRepo).save(captor.capture());
        assertEquals(jwtEntity, captor.getValue());
        LocalDateTime revokedAt = captor.getValue().getRevokedAt();
        assertTrue(revokedAt != null);
    }

    @Test
    void logoutShouldThrowWhenTokenAlreadyRevoked() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("revoked-token");

        when(jwtService.getValidRefreshToken("revoked-token"))
                .thenThrow(new RefreshTokenRevokedException("Refresh token уже отозван"));

        RefreshTokenRevokedException error = assertThrows(
                RefreshTokenRevokedException.class,
                () -> logoutService.logout(request)
        );

        assertEquals("Refresh token уже отозван", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
    }

    @Test
    void logoutShouldThrowWhenTokenExpired() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("expired-token");

        when(jwtService.getValidRefreshToken("expired-token"))
                .thenThrow(new TokenValidationException("Refresh token истёк"));

        TokenValidationException error = assertThrows(TokenValidationException.class, () -> logoutService.logout(request));

        assertEquals("Refresh token истёк", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
    }

    @Test
    void logoutShouldThrowWhenTokenDoesNotExist() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("missing-token");

        when(jwtService.getValidRefreshToken("missing-token"))
                .thenThrow(new RefreshTokenNotFoundException("нет токена"));

        RefreshTokenNotFoundException error = assertThrows(
                RefreshTokenNotFoundException.class,
                () -> logoutService.logout(request)
        );

        assertEquals("нет токена", error.getMessage());
        verify(jwtRepo, never()).save(any(JwtEntity.class));
    }
}
