package tn.mathzero.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import tn.mathzero.identity.domain.UserRole;

class JwtServiceTest {
    private static final String SECRET = "change-this-local-development-secret-at-least-32-bytes";

    @Test
    void createsHs256TokensForDownstreamJwtDecoders() {
        UUID userId = UUID.randomUUID();
        JwtService jwtService = new JwtService(SECRET, 900);
        String token = jwtService.createAccessToken(userId, "admin@mathplay.com", Set.of(UserRole.ADMIN));
        var key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        Jws<Claims> parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(parsed.getPayload().getSubject()).isEqualTo(userId.toString());
        assertThat(parsed.getPayload().get("roles")).isEqualTo(List.of("ADMIN"));
    }
}
