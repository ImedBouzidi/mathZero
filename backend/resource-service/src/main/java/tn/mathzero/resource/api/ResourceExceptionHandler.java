package tn.mathzero.resource.api;

import java.util.UUID;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tn.mathzero.resource.service.ResourceException;

@RestControllerAdvice
public class ResourceExceptionHandler {
    @ExceptionHandler(ResourceException.class)
    ResponseEntity<ResourceDtos.ApiError> handleResourceException(ResourceException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ResourceDtos.ApiError(
                        exception.getCode(), exception.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ResourceDtos.ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return ResponseEntity.badRequest()
                .body(new ResourceDtos.ApiError(
                        "INVALID_REQUEST", message, UUID.randomUUID().toString()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ResourceDtos.ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(new ResourceDtos.ApiError(
                        "INVALID_REQUEST", exception.getMessage(), UUID.randomUUID().toString()));
    }
}
