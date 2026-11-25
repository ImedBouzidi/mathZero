package tn.mathzero.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tn.mathzero.identity.security.AuthCsrfFilter;
import tn.mathzero.identity.security.AuthException;
import tn.mathzero.identity.security.AuthRateLimitService;
import tn.mathzero.identity.security.JwtService;
import tn.mathzero.identity.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "mathzero_refresh";

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthRateLimitService rateLimitService;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            AuthRateLimitService rateLimitService,
            @Value("${app.auth.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.auth.cookie.same-site:Lax}") String cookieSameSite) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @GetMapping("/csrf")
    public ResponseEntity<AuthDtos.ApiResponse<Void>> csrf(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from(AuthCsrfFilter.COOKIE_NAME, "")
                        .secure(cookieSecure)
                        .sameSite(cookieSameSite)
                        .path("/api/v1/auth")
                        .maxAge(0L)
                        .build()
                        .toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie(token).toString());
        return ResponseEntity.ok(response(null));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.ApiResponse<AuthDtos.AuthResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        rateLimitService.checkRegister(clientKey(httpRequest));
        AuthService.AuthResult result = authService.register(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.status(201).body(response(result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.ApiResponse<AuthDtos.AuthResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        rateLimitService.checkLogin(clientKey(httpRequest));
        AuthService.AuthResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(response(result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.ApiResponse<AuthDtos.AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(response(result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthDtos.ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(response(null));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<AuthDtos.ApiResponse<Void>> logoutAll(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            HttpServletResponse response) {
        authService.logoutAll(jwtService.parseUserId(authorization));
        clearRefreshCookie(response);
        return ResponseEntity.ok(response(null));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.ApiResponse<AuthDtos.UserResponse>> me(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        UUID userId = jwtService.parseUserId(authorization);
        return ResponseEntity.ok(response(authService.currentUser(userId)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<AuthDtos.ApiResponse<AuthDtos.UserResponse>> profile(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody AuthDtos.ProfileRequest request) {
        UUID userId = jwtService.parseUserId(authorization);
        return ResponseEntity.ok(response(authService.updateProfile(userId, request)));
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(token).toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshCookie("").mutate().maxAge(0L).build().toString());
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(cookieSecure ? 30 * 86400 : 30 * 86400)
                .build();
    }

    private ResponseCookie csrfCookie(String token) {
        return ResponseCookie.from(AuthCsrfFilter.COOKIE_NAME, token)
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(30 * 86400)
                .build();
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static <T> AuthDtos.ApiResponse<T> response(T data) {
        return new AuthDtos.ApiResponse<>(data, new AuthDtos.ApiResponse.ApiMeta(UUID.randomUUID().toString()));
    }
}
