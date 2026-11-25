package tn.mathzero.curriculum.api;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tn.mathzero.curriculum.service.CurriculumException;

@RestControllerAdvice
public class CurriculumExceptionHandler {
    @ExceptionHandler(CurriculumException.class)
    ResponseEntity<CurriculumDtos.ApiError> handleCurriculumException(CurriculumException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new CurriculumDtos.ApiError(
                        exception.getCode(), exception.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<CurriculumDtos.ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return ResponseEntity.badRequest()
                .body(new CurriculumDtos.ApiError(
                        "INVALID_REQUEST", message, UUID.randomUUID().toString()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<CurriculumDtos.ApiError> handleConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409)
                .body(new CurriculumDtos.ApiError(
                        "CONFLICT", "The requested curriculum record conflicts with existing data",
                        UUID.randomUUID().toString()));
    }
}
