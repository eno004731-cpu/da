package legal_website.Services.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import legal_website.Dto.google.GoogleFlowDto;
import legal_website.Dto.verityEmail.VerityEmailPayload;
import legal_website.EntityAndRepo.Auth.UserEntity;
import legal_website.EntityAndRepo.Auth.UserRepo;
import legal_website.EntityAndRepo.Jwt.JwtEntity;
import legal_website.EntityAndRepo.Jwt.JwtRepo;
import legal_website.EntityAndRepo.verification_codes.VerificationCodeEntity;
import legal_website.common.errors.User.InactiveUserException;
import legal_website.common.errors.User.UserNotFoundException;
import legal_website.common.errors.token.RefreshTokenNotFoundException;
import legal_website.common.errors.token.RefreshTokenRevokedException;
import legal_website.common.errors.token.TokenValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtRepo jwtRepo;
    private final UserRepo userRepo;
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-minutes}")
    private long accessMinutes;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public Date getRefreshTokenExpirationDate() {
        return new Date(System.currentTimeMillis() + accessMinutes * 60_000);
    }

    public String generateAccessToken(UserEntity user) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + accessMinutes * 60_000);
        
        return Jwts.builder()
                // Кого токен представляет
                .subject(user.getEmail())

                // Дополнительные данные
                .claim("userId", user.getId())
                .claim("role", user.getRole())

                // Когда токен создан
                .issuedAt(issuedAt)

                // Когда токен истечёт
                .expiration(expiresAt)

                // Подписываем токен секретным ключом
                .signWith(getSigningKey())

                // Создаём строку токена
                .compact();
    }
    public String generateFlowToken(GoogleFlowDto dto){
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + accessMinutes * 60_000);
        return Jwts.builder()
        .subject(dto.getSub())
        .claim("type", dto.getType())
        .claim("provider", dto.getProvider())
        .claim("email", dto.getEmail())
        .claim("emailVerified", dto.isEmailVerified())
        .claim("name", dto.getFullName())
        .issuedAt(issuedAt)
        .expiration(expiresAt)
        .signWith(getSigningKey())
        .compact();

    }
    public String generateLinkForVerifyEmail(VerityEmailPayload payload,VerificationCodeEntity codeEntity){
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + accessMinutes * 60_000);
        
        
        String token = Jwts.builder()
        .subject(payload.getUserId().toString())
        .claim("purpose",payload.getPurpose())
        .claim("email", payload.getEmail())
        .claim("verificationCodeId",payload.getVerificationCodeId())
        .claim("codeHash", codeEntity.getCodeHash())
        .issuedAt(issuedAt)
        .expiration(expiresAt)
        .signWith(getSigningKey())
        .compact();
        String verificationLink = UriComponentsBuilder
            .fromUriString(frontendBaseUrl)
            .path("/verify-email")
            .queryParam("token", token)
            .build()
            .toUriString();
        return verificationLink;
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessMinutes * 60;
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[32];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

    }


    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хеширования токена", e);
        }
    }

    public boolean isValidRefreshToken(String token) {
        String tokenHash = hashToken(token);
        JwtEntity jwtEntity = jwtRepo.findByTokenHash(tokenHash)
            .orElseThrow(() -> new RefreshTokenNotFoundException("нет токена"));
        if (jwtEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenValidationException("Refresh token истёк");
        }
        if (jwtEntity.getRevokedAt() != null) {
            throw new RefreshTokenRevokedException("Refresh token уже отозван");
        }
        return true;
    }

    public JwtEntity getValidRefreshToken(String token) {
        String tokenHash = hashToken(token);
        JwtEntity jwtEntity = jwtRepo.findByTokenHash(tokenHash)
            .orElseThrow(() -> new RefreshTokenNotFoundException("нет токена"));
        if (jwtEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenValidationException("Refresh token истёк");
        }
        if (jwtEntity.getRevokedAt() != null) {
            throw new RefreshTokenRevokedException("Refresh token уже отозван");
        }
        return jwtEntity;
    }

    public UserEntity getUserFromValidAccessToken(String token){
        Claims jwt = extractAllClaims(token);
        UserEntity user = userRepo.findById(jwt.get("userId",Long.class))
            .orElseThrow(() -> new UserNotFoundException("нет пользователя"));
        if (!user.isActive()) {
            throw new InactiveUserException("пользователь не действителен");
        }
        return user;
    }
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                // Проверяем подпись токена тем же секретным ключом,
                // которым ты его подписывал.
                .verifyWith(getSigningKey())

                // Собираем parser
                .build()

                // Парсим подписанный JWT
                .parseSignedClaims(token)

                // Берём payload
                .getPayload();
    }
    public String getEmail(String token){
        return extractAllClaims(token).getSubject();
    }
    public String getRole(String token){
        return extractAllClaims(token).get("role",String.class);
    }
    public Long getId(String token){
        return extractAllClaims(token).get("userId",Long.class);
    }

    
    private SecretKey getSigningKey() {
        byte[] keyBytes = HexFormat.of().parseHex(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
