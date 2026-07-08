package lawyer_service.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lawyer_service.dto.AuthRequest;
import lawyer_service.dto.AuthResponse;
import lawyer_service.dto.DtoFactory;
import lawyer_service.dto.EntityFactory;
import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.LawyerRepo;
import lawyer_service.repo_entity.enums.Role;
import lawyer_service.repo_entity.enums.StatusLawyer;
import lawyer_service.repo_entity.enums.TokenType;
import lawyer_service.repo_entity.jwt.JwtTokenEntity;
import lawyer_service.repo_entity.jwt.JwtTokenRepo;

class RegisterServiceTest {
    private static final UUID LAWYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "lawyer@example.com";
    private static final String RAW_PASSWORD = "StrongPassword123!";
    private static final String IP = "192.168.1.10";
    private static final String AGENT_ID = "Mozilla/5.0";
    private static final String JWT_SECRET = Base64.getEncoder()
        .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldRegisterLawyerAndReturnValidTokens() {
        RegisterTestContext context = registerTestContext();
        AuthRequest request = authRequest();
        when(context.lawyerRepo().existsByEmail(EMAIL)).thenReturn(false);
        when(context.lawyerRepo().save(any(LawyerEntity.class))).thenAnswer(invocation -> {
            LawyerEntity lawyer = invocation.getArgument(0);
            lawyer.setId(LAWYER_ID);
            return lawyer;
        });

        AuthResponse response = context.registerService().reg(request);

        assertThat(response.getLaweyr()).isEqualTo(LAWYER_ID);
        assertThat(response.getRole()).isEqualTo(Role.LAWYER);
        assertThat(response.getStatus()).isEqualTo(StatusLawyer.PENDING_VERIFICATION);
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getAccessToken()).isNotEqualTo(response.getRefreshToken());
        assertThat(response.getLawyerInformation().getEmail()).isEqualTo(EMAIL);
        assertThat(response.getLawyerInformation().getFirstName()).isEqualTo(request.getFirstName());
        assertThat(response.getLawyerInformation().getLastName()).isEqualTo(request.getLastName());
        assertThat(response.getLawyerInformation().getMiddleName()).isEqualTo(request.getMiddleName());
        assertThat(response.getLawyerInformation().getPhone()).isEqualTo(request.getPhone());
        assertThat(response.getLawyerInformation().getSpecialization()).isEqualTo(request.getSpecialization());

        ArgumentCaptor<LawyerEntity> lawyerCaptor = ArgumentCaptor.forClass(LawyerEntity.class);
        verify(context.lawyerRepo()).save(lawyerCaptor.capture());
        LawyerEntity savedLawyer = lawyerCaptor.getValue();
        assertThat(savedLawyer.getEmail()).isEqualTo(EMAIL);
        assertThat(savedLawyer.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(context.passwordEncoder().matches(RAW_PASSWORD, savedLawyer.getPasswordHash())).isTrue();
        assertThat(savedLawyer.getRole()).isEqualTo(Role.LAWYER);
        assertThat(savedLawyer.getStatus()).isEqualTo(StatusLawyer.PENDING_VERIFICATION);

        Claims refreshClaims = parseToken(response.getRefreshToken());
        assertTokenClaims(refreshClaims, TokenType.REFRESH);
        Claims accessClaims = parseToken(response.getAccessToken());
        assertTokenClaims(accessClaims, TokenType.ACCESS);
        assertThat(accessClaims.getExpiration()).isBefore(refreshClaims.getExpiration());
        assertThat(context.tokenService().getEmailByAcsessToken(response.getAccessToken())).contains(EMAIL);
        assertThat(context.tokenService().getEmailByAcsessToken(response.getRefreshToken())).isEmpty();

        ArgumentCaptor<JwtTokenEntity> tokenCaptor = ArgumentCaptor.forClass(JwtTokenEntity.class);
        verify(context.jwtTokenRepo()).save(tokenCaptor.capture());
        JwtTokenEntity savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getLawyer()).isSameAs(savedLawyer);
        assertThat(savedToken.getTokenHash()).isEqualTo(sha256(response.getRefreshToken()));
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.isSessionBlocked()).isFalse();
        assertThat(savedToken.getReplacedByToken()).isNull();
        assertThat(savedToken.getIpAddress()).isEqualTo(IP);
        assertThat(savedToken.getUserAgent()).isEqualTo(AGENT_ID);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldThrowConflictAndNotSaveAnythingWhenEmailAlreadyExists() {
        RegisterTestContext context = registerTestContext();
        AuthRequest request = authRequest();
        when(context.lawyerRepo().existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> context.registerService().reg(request))
            .isInstanceOf(HttpStatusCodeException.class)
            .extracting(exception -> ((HttpStatusCodeException) exception).getStatusCode().value())
            .isEqualTo(409);

        verify(context.lawyerRepo(), never()).save(any(LawyerEntity.class));
        verifyNoInteractions(context.jwtTokenRepo());
    }

    @Test
    void shouldPropagateErrorAndNotPersistRefreshTokenWhenSaveTokenFails() {
        RegisterTestContext context = registerTestContext();
        AuthRequest request = authRequest();
        RuntimeException saveTokenException = new RuntimeException("Token storage is unavailable");
        when(context.lawyerRepo().existsByEmail(EMAIL)).thenReturn(false);
        when(context.lawyerRepo().save(any(LawyerEntity.class))).thenAnswer(invocation -> {
            LawyerEntity lawyer = invocation.getArgument(0);
            lawyer.setId(LAWYER_ID);
            return lawyer;
        });
        doThrow(saveTokenException)
            .when(context.tokenService())
            .saveToken(any(LawyerEntity.class), any(String.class), eq(IP), eq(AGENT_ID));

        assertThatThrownBy(() -> context.registerService().reg(request))
            .isSameAs(saveTokenException);

        verify(context.lawyerRepo()).save(any(LawyerEntity.class));
        verify(context.jwtTokenRepo(), never()).save(any(JwtTokenEntity.class));
    }

    private RegisterTestContext registerTestContext() {
        LawyerRepo lawyerRepo = mock(LawyerRepo.class);
        JwtTokenRepo jwtTokenRepo = mock(JwtTokenRepo.class);
        EntityFactory entityFactory = new EntityFactory();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        DtoFactory dtoFactory = new DtoFactory();
        TokenService tokenService = new TokenService(entityFactory, jwtTokenRepo);
        ReflectionTestUtils.setField(tokenService, "secret", JWT_SECRET);
        TokenService tokenServiceSpy = org.mockito.Mockito.spy(tokenService);

        RegisterService registerService = new RegisterService(
            lawyerRepo,
            entityFactory,
            passwordEncoder,
            dtoFactory,
            tokenServiceSpy
        );

        return new RegisterTestContext(
            registerService,
            lawyerRepo,
            jwtTokenRepo,
            passwordEncoder,
            tokenServiceSpy
        );
    }

    private AuthRequest authRequest() {
        AuthRequest request = new AuthRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        request.setFirstName("Nikita");
        request.setLastName("Ivanov");
        request.setMiddleName("Sergeevich");
        request.setPhone("+79990001122");
        request.setSpecialization("Civil law");
        request.setIp(IP);
        request.setAgentId(AGENT_ID);
        return request;
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(JWT_SECRET));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private void assertTokenClaims(Claims claims, TokenType tokenType) {
        assertThat(claims.get("email", String.class)).isEqualTo(EMAIL);
        assertThat(claims.get("lawyerId", String.class)).isEqualTo(LAWYER_ID.toString());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.LAWYER.name());
        assertThat(claims.get("status", String.class)).isEqualTo(StatusLawyer.PENDING_VERIFICATION.name());
        assertThat(claims.get("tokenType", String.class)).isEqualTo(tokenType.name());
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
    }

    private String sha256(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing algorithm is not available", e);
        }
    }

    private record RegisterTestContext(
        RegisterService registerService,
        LawyerRepo lawyerRepo,
        JwtTokenRepo jwtTokenRepo,
        BCryptPasswordEncoder passwordEncoder,
        TokenService tokenService
    ) {
    }
}
