package document_service.configs;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.HexFormat;

@Service
public class JwtTokenService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    public String extractRole(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = HexFormat.of().parseHex(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
