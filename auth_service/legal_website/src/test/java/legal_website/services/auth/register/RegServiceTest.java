package legal_website.services.auth.register;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import legal_website.dto.register.AuthResponse;
import legal_website.dto.register.AuthUserResponse;
import legal_website.dto.register.RegRequest;
import legal_website.persistence.auth.UserEntity;
import legal_website.persistence.auth.UserRepo;
import legal_website.services.auth.AuthSessionService;

@ExtendWith(MockitoExtension.class)
class RegServiceTest {

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private UserRepo userRepo;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private RegService regService;

    @Test
    void regUserShouldSaveUserTokenAndReturnJwtContract() {
        RegRequest request = new RegRequest();
        request.setFullName("Ivan Ivanov");
        request.setEmail("ivan@test.ru");
        request.setPhone("+79990000000");
        request.setCompanyName("OOO Test");
        request.setPassword("secret");

        AuthUserResponse userResponse = new AuthUserResponse();
        userResponse.setId(42L);
        userResponse.setFullName("Ivan Ivanov");
        userResponse.setEmail("ivan@test.ru");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("access-token");
        authResponse.setRefreshToken("raw-refresh-token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(900L);
        authResponse.setUser(userResponse);

        when(userRepo.existsByEmail("ivan@test.ru")).thenReturn(false);
        when(userRepo.existsByPhone("+79990000000")).thenReturn(false);
        when(encoder.encode("secret")).thenReturn("hashed-password");
        when(authSessionService.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(userRepo.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });
        when(authSessionService.buildUserResponse(any(UserEntity.class))).thenReturn(userResponse);
        when(authSessionService.buildAuthResponse(any(UserEntity.class), any(String.class), any(AuthUserResponse.class)))
                .thenReturn(authResponse);

        AuthResponse response = regService.regUser(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("raw-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(42L, response.getUser().getId());
        assertEquals("Ivan Ivanov", response.getUser().getFullName());
        assertEquals("ivan@test.ru", response.getUser().getEmail());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("CLIENT", savedUser.getRole());
        assertEquals("hashed-password", savedUser.getPasswordHash());
        assertEquals("ivan@test.ru", savedUser.getEmail());
        verify(authSessionService).saveToken(savedUser, "raw-refresh-token");
    }
}
