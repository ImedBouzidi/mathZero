package tn.mathzero.gateway.api;

/**
 * Standard success envelope used by public API endpoints.
 *
 * @param data payload of the response
 * @param meta request metadata
 */
public record ApiResponse<T>(T data, ResponseMeta meta) {

    public static <T> ApiResponse<T> of(T data, String requestId) {
        return new ApiResponse<>(data, new ResponseMeta(requestId));
    }
}
