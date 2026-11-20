package tn.mathzero.gateway.api;

/**
 * Metadata attached to every API response.
 *
 * @param requestId correlation identifier for the request
 */
public record ResponseMeta(String requestId) {
}
