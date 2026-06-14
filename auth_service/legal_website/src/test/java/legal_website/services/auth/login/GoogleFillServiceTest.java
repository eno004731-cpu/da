package legal_website.services.auth.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Claims;
import legal_website.dto.google.GoogleFillRequest;
import legal_website.dto.register.AuthResponse;
import legal_website.dto.register.AuthUserResponse;
import legal_website.persistence.auth.OAuthAccountEntity;
import legal_website.persistence.auth.OAuthAccountRepo;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.persistence.jwt.JwtRepo;
import legal_website.services.jwt.JwtService;
import legal_website.services.auth.AuthSessionService;
import legal_website.common.errors.token.InvalidFlowTokenException;

@ExtendWith(MockitoExtension.class)
class GoogleFillServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OAuthAccountRepo oAuthAccountRepo;

    @Mock
    private JwtRepo jwtRepo;

    @InjectMocks
    private GoogleFillService googleFillService;

    @Test
    void fillGoogleShouldCreateUserWithAuditFieldsAndReturnAuthResponse() {
        GoogleFillRequest request = new GoogleFillRequest();
        request.setFlowToken("flow-token");
        request.setFullName("Е Но");
        request.setPassword("secret123");

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.extractAllClaims("flow-token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("PROFILE_COMPLETION_REQUIRED");
        when(claims.get("provider", String.class)).thenReturn("google");
        when(claims.get("email", String.class)).thenReturn("eno004731@gmail.com");
        when(claims.get("sub", String.class)).thenReturn("google-sub-123");
        when(claims.get("emailVerified", Boolean.class)).thenReturn(true);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");

        AuthUserResponse userResponse = new AuthUserResponse();
        userResponse.setEmail("eno004731@gmail.com");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("access-token");
        authResponse.setRefreshToken("refresh-token");
        authResponse.setUser(userResponse);

        when(authSessionService.generateRefreshToken()).thenReturn("refresh-token");
        when(authSessionService.buildUserResponse(any(UserEntity.class))).thenReturn(userResponse);
        when(authSessionService.buildAuthResponse(any(UserEntity.class), any(String.class), any(AuthUserResponse.class)))
                .thenReturn(authResponse);

        AuthResponse response = googleFillService.fillGoogle(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();

        assertEquals("eno004731@gmail.com", savedUser.getEmail());
        assertEquals("Е Но", savedUser.getFullName());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals("CLIENT", savedUser.getRole());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());

        ArgumentCaptor<OAuthAccountEntity> oauthCaptor = ArgumentCaptor.forClass(OAuthAccountEntity.class);
        verify(oAuthAccountRepo).save(oauthCaptor.capture());
        OAuthAccountEntity savedOauth = oauthCaptor.getValue();

        assertEquals("google", savedOauth.getProvider());
        assertEquals("google-sub-123", savedOauth.getProviderUserId());
        assertEquals("eno004731@gmail.com", savedOauth.getProviderEmail());
        assertNotNull(savedOauth.getCreatedAt());
        assertNotNull(savedOauth.getUpdatedAt());
        assertEquals(savedUser, savedOauth.getUser());

        verify(authSessionService).saveToken(savedUser, "refresh-token");
        assertEquals(authResponse, response);
    }

    @Test
    void fillGoogleShouldThrowWhenFlowTokenIsMalformed() {
        GoogleFillRequest request = new GoogleFillRequest();
        request.setFlowToken("broken-token");
        request.setFullName("Е Но");
        request.setPassword("secret123!");

        when(jwtService.extractAllClaims("broken-token"))
                .thenThrow(new RuntimeException("bad jwt"));

        InvalidFlowTokenException error = assertThrows(
                InvalidFlowTokenException.class,
                () -> googleFillService.fillGoogle(request)
        );

        assertEquals("Invalid flow token", error.getMessage());
        verify(userRepo, never()).save(any());
        verify(oAuthAccountRepo, never()).save(any());
    }

    @Test
    void fillGoogleShouldThrowWhenFlowTokenTypeIsInvalid() {
        GoogleFillRequest request = new GoogleFillRequest();
        request.setFlowToken("flow-token");
        request.setFullName("Е Но");
        request.setPassword("secret123!");

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.extractAllClaims("flow-token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("ACCESS");

        InvalidFlowTokenException error = assertThrows(
                InvalidFlowTokenException.class,
                () -> googleFillService.fillGoogle(request)
        );

        assertEquals("Invalid flow token type", error.getMessage());
    }

    @Test
    void fillGoogleShouldReturnAuthResponseWhenOauthAlreadyLinkedForExistingUser() {
        GoogleFillRequest request = new GoogleFillRequest();
        request.setFlowToken("flow-token");
        request.setFullName("Е Но");
        request.setPassword("secret123!");

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.extractAllClaims("flow-token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("PROFILE_COMPLETION_REQUIRED");
        when(claims.get("provider", String.class)).thenReturn("google");
        when(claims.get("email", String.class)).thenReturn("eno004731@gmail.com");
        when(claims.get("sub", String.class)).thenReturn("google-sub-123");

        UserEntity existingUser = new UserEntity();
        existingUser.setEmail("eno004731@gmail.com");
        existingUser.setActive(true);

        AuthUserResponse userResponse = new AuthUserResponse();
        userResponse.setEmail("eno004731@gmail.com");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("access-token");
        authResponse.setRefreshToken("refresh-token");
        authResponse.setUser(userResponse);

        when(userRepo.findByEmail("eno004731@gmail.com")).thenReturn(java.util.Optional.of(existingUser));
        when(oAuthAccountRepo.existsByProviderAndProviderUserId("google", "google-sub-123")).thenReturn(true);
        when(authSessionService.generateRefreshToken()).thenReturn("refresh-token");
        when(authSessionService.buildUserResponse(existingUser)).thenReturn(userResponse);
        when(authSessionService.buildAuthResponse(existingUser, "refresh-token", userResponse)).thenReturn(authResponse);

        AuthResponse response = googleFillService.fillGoogle(request);

        assertEquals(authResponse, response);
        verify(authSessionService).saveToken(existingUser, "refresh-token");
        verify(userRepo, never()).save(any(UserEntity.class));
        verify(oAuthAccountRepo, never()).save(any(OAuthAccountEntity.class));
    }
}
