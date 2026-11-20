package tn.mathzero.gateway.health;

import java.time.Instant;

/**
 * Public health snapshot of the API Gateway.
 *
 * @param status    platform status reported by the Gateway
 * @param service   logical service name
 * @param version   deployed build version
 * @param timestamp server time of the health check
 */
public record GatewayHealth(String status, String service, String version, Instant timestamp) {
}
