package lawyer_service.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lawyer_service.dto.EntityFactory;
import lawyer_service.repo_entity.LawyerEntity;
import lawyer_service.repo_entity.enums.TokenType;
import lawyer_service.repo_entity.jwt.JwtTokenEntity;
import lawyer_service.repo_entity.jwt.JwtTokenRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/*
класс для генерации проверки и сохранения токенов 
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    private final EntityFactory entityFactory;
    private final JwtTokenRepo jwtTokenRepo;
    @Value("${JWT_SECRET}")
    private String secret;
    private SecretKey key;
    private final long expiresDay = 15*86400;
    private final long accessTokenExpiresSeconds = 15*60;

    public String buildAccessToken(LawyerEntity lawyer){
        return buildToken(lawyer, accessTokenExpiresSeconds,TokenType.ACCESS);
    }

    public String buidlRefreshToken(LawyerEntity lawyer){
        return buildToken(lawyer, expiresDay,TokenType.REFRESH);
    }

    private String buildToken(LawyerEntity lawyer, long expiresSeconds,TokenType tokenType){
        return Jwts.builder()
            .claim("email", lawyer.getEmail())
            .claim("lawyerId", lawyer.getId())
            .claim("status", lawyer.getStatus())
            .claim("role", lawyer.getRole())
            .claim("tokenType", tokenType.name())
            .expiration(Date.from(Instant.now().plusSeconds(expiresSeconds)))
            .signWith(getKey())
            .compact();
    }

    private Claims getToken(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public Optional<String> getEmailByAcsessToken(String token){
        
        Claims parseToken;
        try {
            Claims parseToken1 = getToken(token);
            parseToken = parseToken1;
        } catch (Exception e) {
            return Optional.empty();
        }
        if (parseToken.getExpiration().before(Date.from(Instant.now()))) {
            return Optional.empty();
        }
        if (!TokenType.ACCESS.name().equals(parseToken.get("tokenType", String.class))) {
            return Optional.empty();
        }
        String email = parseToken.get("email",String.class);
        return Optional.ofNullable(email);
    }
    @Transactional
    public void saveToken(LawyerEntity lawyer,String token,String ip,String agentId){
        JwtTokenEntity tokenEntity = entityFactory.buildJwtEntity(lawyer, expiresDay, getHash(token), ip, agentId);
        jwtTokenRepo.save(tokenEntity);
    }

    private String getHash(String token){
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 hashing algorithm is not available", e);
        }
    }

    private SecretKey getKey(){
        if (key != null) {
            return key;
        }
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        key = Keys.hmacShaKeyFor(keyBytes);
        return key;
    }
}
