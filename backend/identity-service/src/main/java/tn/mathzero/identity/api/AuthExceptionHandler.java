package tn.mathzero.identity.api;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tn.mathzero.identity.security.AuthException;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<AuthDtos.ErrorResponse> handleAuth(AuthException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "UNAUTHORIZED", "REFRESH_REUSE", "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "EMAIL_EXISTS" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(new AuthDtos.ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<AuthDtos.ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<AuthDtos.ErrorResponse> handleMissing(NoSuchElementException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<AuthDtos.ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthDtos.ErrorResponse("INTERNAL_ERROR", "The service could not complete the request"));
    }
}
