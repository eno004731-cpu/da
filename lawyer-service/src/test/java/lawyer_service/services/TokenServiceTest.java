package lawyer_service.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lawyer_service.dto.EntityFactory;
import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.enums.Role;
import lawyer_service.repo_entity.enums.StatusLawyer;
import lawyer_service.repo_entity.enums.TokenType;
import lawyer_service.repo_entity.jwt.JwtTokenEntity;
import lawyer_service.repo_entity.jwt.JwtTokenRepo;

class TokenServiceTest {
    private static final UUID LAWYER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "lawyer@example.com";
    private static final String IP = "10.0.0.10";
    private static final String AGENT_ID = "Chrome/126";
    private static final String JWT_SECRET = Base64.getEncoder()
        .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    // Positive: access token contains ACCESS type and all lawyer claims.
    @Test
    void buildAccessTokenShouldReturnTokenWithAccessTypeAndLawyerClaims() {
        TokenServiceContext context = tokenServiceContext();
        LawyerEntity lawyer = lawyer();

        String token = context.tokenService().buildAccessToken(lawyer);

        Claims claims = parseToken(token);
        assertTokenClaims(claims, TokenType.ACCESS);
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
    }

    // Positive: refresh token contains REFRESH type and all lawyer claims.
    @Test
    void buidlRefreshTokenShouldReturnTokenWithRefreshTypeAndLawyerClaims() {
        TokenServiceContext context = tokenServiceContext();
        LawyerEntity lawyer = lawyer();

        String token = context.tokenService().buidlRefreshToken(lawyer);

        Claims claims = parseToken(token);
        assertTokenClaims(claims, TokenType.REFRESH);
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
    }

    // Positive: access and refresh tokens are different and have different lifetimes.
    @Test
    void buildTokensShouldReturnDifferentTokensWithDifferentTypesAndExpiration() {
        TokenServiceContext context = tokenServiceContext();
        LawyerEntity lawyer = lawyer();

        String accessToken = context.tokenService().buildAccessToken(lawyer);
        String refreshToken = context.tokenService().buidlRefreshToken(lawyer);

        Claims accessClaims = parseToken(accessToken);
        Claims refreshClaims = parseToken(refreshToken);
        assertThat(accessToken).isNotEqualTo(refreshToken);
        assertTokenClaims(accessClaims, TokenType.ACCESS);
        assertTokenClaims(refreshClaims, TokenType.REFRESH);
        assertThat(accessClaims.getExpiration()).isBefore(refreshClaims.getExpiration());
    }

    // Positive: valid non-expired ACCESS token returns lawyer email.
    @Test
    void getEmailByAcsessTokenShouldReturnEmailForValidNotExpiredAccessToken() {
        TokenServiceContext context = tokenServiceContext();
        String accessToken = context.tokenService().buildAccessToken(lawyer());

        assertThat(context.tokenService().getEmailByAcsessToken(accessToken))
            .contains(EMAIL);
    }

    // Negative: REFRESH token must not be accepted as ACCESS token.
    @Test
    void getEmailByAcsessTokenShouldReturnEmptyWhenTokenTypeIsNotAccess() {
        TokenServiceContext context = tokenServiceContext();
        String refreshToken = context.tokenService().buidlRefreshToken(lawyer());

        assertThat(context.tokenService().getEmailByAcsessToken(refreshToken))
            .isEmpty();
    }

    // Negative: expired ACCESS token does not return email.
    @Test
    void getEmailByAcsessTokenShouldReturnEmptyWhenTokenIsExpired() {
        TokenServiceContext context = tokenServiceContext();
        String expiredAccessToken = buildToken(lawyer(), TokenType.ACCESS, -60);

        assertThat(context.tokenService().getEmailByAcsessToken(expiredAccessToken))
            .isEmpty();
    }

    // Negative: malformed token is rejected.
    @Test
    void getEmailByAcsessTokenShouldReturnEmptyWhenTokenIsInvalid() {
        TokenServiceContext context = tokenServiceContext();

        assertThat(context.tokenService().getEmailByAcsessToken("not-a-jwt"))
            .isEmpty();
    }

    // Negative: ACCESS token without email claim is rejected.
    @Test
    void getEmailByAcsessTokenShouldReturnEmptyWhenAccessTokenDoesNotContainEmail() {
        TokenServiceContext context = tokenServiceContext();
        String tokenWithoutEmail = Jwts.builder()
            .claim("lawyerId", LAWYER_ID)
            .claim("status", StatusLawyer.PENDING_VERIFICATION.name())
            .claim("role", Role.LAWYER.name())
            .claim("tokenType", TokenType.ACCESS.name())
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(signingKey())
            .compact();

        assertThat(context.tokenService().getEmailByAcsessToken(tokenWithoutEmail))
            .isEmpty();
    }

    // Positive: refresh token is hashed and persisted as JwtTokenEntity.
    @Test
    void saveTokenShouldHashTokenAndPersistJwtTokenEntity() {
        TokenServiceContext context = tokenServiceContext();
        LawyerEntity lawyer = lawyer();
        String refreshToken = context.tokenService().buidlRefreshToken(lawyer);

        context.tokenService().saveToken(lawyer, refreshToken, IP, AGENT_ID);

        ArgumentCaptor<JwtTokenEntity> tokenCaptor = ArgumentCaptor.forClass(JwtTokenEntity.class);
        verify(context.jwtTokenRepo()).save(tokenCaptor.capture());
        JwtTokenEntity savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getLawyer()).isSameAs(lawyer);
        assertThat(savedToken.getTokenHash()).isEqualTo(sha256(refreshToken));
        assertThat(savedToken.getTokenHash()).isNotEqualTo(refreshToken);
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getIpAddress()).isEqualTo(IP);
        assertThat(savedToken.getUserAgent()).isEqualTo(AGENT_ID);
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getLastUsedAt()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.getReplacedByToken()).isNull();
        assertThat(savedToken.isSessionBlocked()).isFalse();
        assertThat(savedToken.getSessionBlockedAt()).isNull();
        assertThat(savedToken.getSessionBlockedReason()).isNull();
    }

    // Negative: repository failure is propagated to caller.
    @Test
    void saveTokenShouldPropagateRepositoryException() {
        TokenServiceContext context = tokenServiceContext();
        LawyerEntity lawyer = lawyer();
        String refreshToken = context.tokenService().buidlRefreshToken(lawyer);
        RuntimeException repositoryException = new RuntimeException("DB is unavailable");
        when(context.jwtTokenRepo().save(any(JwtTokenEntity.class))).thenThrow(repositoryException);

        assertThatThrownBy(() -> context.tokenService().saveToken(lawyer, refreshToken, IP, AGENT_ID))
            .isSameAs(repositoryException);
    }

    private TokenServiceContext tokenServiceContext() {
        JwtTokenRepo jwtTokenRepo = mock(JwtTokenRepo.class);
        TokenService tokenService = new TokenService(new EntityFactory(), jwtTokenRepo);
        ReflectionTestUtils.setField(tokenService, "secret", JWT_SECRET);
        return new TokenServiceContext(tokenService, jwtTokenRepo);
    }

    private LawyerEntity lawyer() {
        LawyerEntity lawyer = new LawyerEntity();
        lawyer.setId(LAWYER_ID);
        lawyer.setEmail(EMAIL);
        lawyer.setRole(Role.LAWYER);
        lawyer.setStatus(StatusLawyer.PENDING_VERIFICATION);
        lawyer.setFirstName("Nikita");
        lawyer.setLastName("Ivanov");
        return lawyer;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private String buildToken(LawyerEntity lawyer, TokenType tokenType, long expiresInSeconds) {
        return Jwts.builder()
            .claim("email", lawyer.getEmail())
            .claim("lawyerId", lawyer.getId())
            .claim("status", lawyer.getStatus().name())
            .claim("role", lawyer.getRole().name())
            .claim("tokenType", tokenType.name())
            .expiration(Date.from(Instant.now().plusSeconds(expiresInSeconds)))
            .signWith(signingKey())
            .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(JWT_SECRET));
    }

    private void assertTokenClaims(Claims claims, TokenType tokenType) {
        assertThat(claims.get("email", String.class)).isEqualTo(EMAIL);
        assertThat(claims.get("lawyerId", String.class)).isEqualTo(LAWYER_ID.toString());
        assertThat(claims.get("status", String.class)).isEqualTo(StatusLawyer.PENDING_VERIFICATION.name());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.LAWYER.name());
        assertThat(claims.get("tokenType", String.class)).isEqualTo(tokenType.name());
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

    private record TokenServiceContext(
        TokenService tokenService,
        JwtTokenRepo jwtTokenRepo
    ) {
    }
}
