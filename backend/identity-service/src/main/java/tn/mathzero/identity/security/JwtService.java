package tn.mathzero.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tn.mathzero.identity.domain.UserRole;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTokenSeconds;

    public JwtService(
            @Value("${app.auth.jwt-secret}") String secret,
            @Value("${app.auth.access-token-seconds:900}") long accessTokenSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("AUTH_JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public long accessTokenSeconds() {
        return accessTokenSeconds;
    }

    public String createAccessToken(UUID userId, String email, Set<UserRole> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles.stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public UUID parseUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthException("UNAUTHORIZED", "Authentication is required");
        }
        String token = authorizationHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception exception) {
            throw new AuthException("UNAUTHORIZED", "The access token is invalid or expired");
        }
        return UUID.fromString(claims.getSubject());
    }
}
