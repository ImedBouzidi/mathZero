package tn.mathzero.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

import tn.mathzero.identity.domain.UserRole;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(min = 2, max = 80) String displayName,
            String gradeId,
            String gradeLabel,
            String cycle,
            String locale) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password) {}

    public record ProfileRequest(
            @Size(max = 80) String displayName,
            @Size(max = 80) String gradeId,
            @Size(max = 120) String gradeLabel,
            @Size(max = 40) String cycle,
            @Size(max = 10) String locale,
            @Size(max = 2048) String avatarUrl) {}

    public record UserResponse(
            UUID id,
            String email,
            String displayName,
            Set<UserRole> roles,
            ProfileResponse profile) {}

    public record ProfileResponse(
            UUID id,
            String displayName,
            String gradeId,
            String gradeLabel,
            String cycle,
            String locale,
            String avatarUrl) {}

    public record AuthResponse(
            String accessToken,
            long expiresIn,
            UserResponse user) {}

    public record ApiResponse<T>(T data, ApiMeta meta) {
        public record ApiMeta(String requestId) {}
    }

    public record ErrorResponse(String code, String message) {}
}
