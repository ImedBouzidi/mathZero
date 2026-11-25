package tn.mathzero.curriculum.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class CurriculumSecurityConfigTest {
    @Test
    void convertsTheIdentityRolesClaimIntoSpringAuthorities() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("roles", List.of("ADMIN"))
                .build();

        var authentication = new CurriculumSecurityConfig()
                .curriculumJwtAuthenticationConverter()
                .convert(jwt);

        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
