package tn.mathzero.curriculum.service;

import org.springframework.http.HttpStatus;

public class CurriculumException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public CurriculumException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    public static CurriculumException notFound(String resource) {
        return new CurriculumException("NOT_FOUND", resource + " was not found", HttpStatus.NOT_FOUND);
    }

    public static CurriculumException conflict(String message) {
        return new CurriculumException("CONFLICT", message, HttpStatus.CONFLICT);
    }

    public static CurriculumException invalid(String message) {
        return new CurriculumException("INVALID_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
}
