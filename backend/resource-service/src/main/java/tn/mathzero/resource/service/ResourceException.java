package tn.mathzero.resource.service;

import org.springframework.http.HttpStatus;

public class ResourceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ResourceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static ResourceException invalid(String message) {
        return new ResourceException(HttpStatus.BAD_REQUEST, "INVALID_RESOURCE", message);
    }

    public static ResourceException notFound() {
        return new ResourceException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found");
    }

    public static ResourceException conflict(String message) {
        return new ResourceException(HttpStatus.CONFLICT, "INVALID_REVIEW_TRANSITION", message);
    }
}
