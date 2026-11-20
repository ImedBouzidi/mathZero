package tn.mathzero.gateway.health;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import tn.mathzero.gateway.api.ApiResponse;
import tn.mathzero.gateway.web.RequestIdWebFilter;

/**
 * Lightweight, browser-facing readiness endpoint.
 *
 * <p>Actuator remains the operational health endpoint; this endpoint lets the
 * Angular shell verify that it can reach the Gateway through the public API
 * contract used by every later feature.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final String version;

    public HealthController(@Value("${app.version:0.1.0}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public Mono<ApiResponse<GatewayHealth>> health(ServerWebExchange exchange) {
        String requestId = exchange.getAttributeOrDefault(
                RequestIdWebFilter.REQUEST_ID_ATTRIBUTE, UUID.randomUUID().toString());
        GatewayHealth health = new GatewayHealth("UP", "gateway-service", version, Instant.now());
        return Mono.just(ApiResponse.of(health, requestId));
    }
}
