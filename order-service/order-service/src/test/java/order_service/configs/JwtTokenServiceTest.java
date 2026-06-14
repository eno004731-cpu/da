package order_service.configs;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceTest {
    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void extractUserId_readsUserIdClaimFromAccessToken() {
        JwtTokenService service = new JwtTokenService();
        ReflectionTestUtils.setField(service, "jwtSecret", SECRET);

        String token = Jwts.builder()
                .subject("client@example.com")
                .claim("userId", 42L)
                .claim("role", "CLIENT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(HexFormat.of().parseHex(SECRET)))
                .compact();

        assertEquals(42L, service.extractUserId(token));
        assertEquals("CLIENT", service.extractRole(token));
    }
}
