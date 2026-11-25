package tn.mathzero.gateway.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
public class ResourceServerConfig {
    @Bean
    SecurityWebFilterChain gatewaySecurity(
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                HttpMethod.OPTIONS,
                                "/api/**")
                        .permitAll()
                        .pathMatchers(
                                "/actuator/**",
                                "/api/v1/health",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/curricula",
                                "/api/v1/curricula/**",
                                "/api/v1/versions/**",
                                "/api/v1/levels/**",
                                "/api/v1/grades/**",
                                "/api/v1/subjects/**",
                                "/api/v1/chapters/**",
                                "/api/v1/skills/**",
                                "/api/v1/resources",
                                "/api/v1/resources/**", "/api/v1/learning/**")
                        .permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/learning/**")
                        .permitAll()
                        .pathMatchers("/api/v1/**")
                        .authenticated()
                        .anyExchange()
                        .permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${app.auth.jwt-secret}") String secret) {
        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }
}
