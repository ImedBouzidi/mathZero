package tn.mathzero.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.mathzero.identity.api.AuthDtos;
import tn.mathzero.identity.domain.IdentityProfile;
import tn.mathzero.identity.domain.IdentityUser;
import tn.mathzero.identity.domain.RefreshSession;
import tn.mathzero.identity.domain.TokenRecord;
import tn.mathzero.identity.domain.UserRole;
import tn.mathzero.identity.repository.IdentityProfileRepository;
import tn.mathzero.identity.repository.IdentityUserRepository;
import tn.mathzero.identity.repository.RefreshSessionRepository;
import tn.mathzero.identity.repository.TokenRecordRepository;
import tn.mathzero.identity.security.AuthException;
import tn.mathzero.identity.security.JwtService;

@Service
public class AuthService {
    public record AuthResult(AuthDtos.AuthResponse response, String refreshToken) {}

    private final IdentityUserRepository users;
    private final IdentityProfileRepository profiles;
    private final RefreshSessionRepository refreshSessions;
    private final TokenRecordRepository tokenRecords;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenDays;

    public AuthService(
            IdentityUserRepository users,
            IdentityProfileRepository profiles,
            RefreshSessionRepository refreshSessions,
            TokenRecordRepository tokenRecords,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.auth.refresh-token-days:30}") long refreshTokenDays) {
        this.users = users;
        this.profiles = profiles;
        this.refreshSessions = refreshSessions;
        this.tokenRecords = tokenRecords;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthResult register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (users.existsByNormalizedEmail(normalizedEmail)) {
            throw new AuthException("EMAIL_EXISTS", "An account already exists for this email");
        }

        Instant now = Instant.now();
        IdentityUser user = new IdentityUser();
        user.setId(UUID.randomUUID());
        user.setNormalizedEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setRoles(Set.of(UserRole.STUDENT));
        users.save(user);

        IdentityProfile profile = new IdentityProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setGradeId(request.gradeId());
        profile.setGradeLabel(request.gradeLabel());
        profile.setCycle(request.cycle());
        profile.setLocale(normalizeLocale(request.locale()));
        profile.setUpdatedAt(now);
        profiles.save(profile);

        return issueSession(user);
    }

    @Transactional
    public AuthResult login(AuthDtos.LoginRequest request) {
        IdentityUser user = users.findByNormalizedEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "Email or password is incorrect"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS", "Email or password is incorrect");
        }
        return issueSession(user);
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException("UNAUTHORIZED", "A refresh session is required");
        }
        String tokenHash = sha256(refreshToken);
        RefreshSession existing = refreshSessions.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("UNAUTHORIZED", "The refresh session is invalid"));
        Instant now = Instant.now();

        if (existing.getStatus() != RefreshSession.Status.ACTIVE
                || existing.getExpiresAt().isBefore(now)) {
            existing.setStatus(RefreshSession.Status.REUSED);
            existing.setRevokedAt(now);
            refreshSessions.save(existing);
            revokeAllSessions(existing.getUserId(), now);
            throw new AuthException("REFRESH_REUSE", "The refresh session was reused; sign in again");
        }

        IdentityUser user = users.findById(existing.getUserId())
                .orElseThrow(() -> new AuthException("UNAUTHORIZED", "The account no longer exists"));
        String replacementToken = issueRefreshToken(existing);
        return new AuthResult(issueAccessOnly(user), replacementToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        RefreshSession session = refreshSessions.findByTokenHash(sha256(refreshToken)).orElse(null);
        if (session != null) {
            session.setStatus(RefreshSession.Status.REVOKED);
            session.setRevokedAt(Instant.now());
            refreshSessions.save(session);
        }
    }

    @Transactional
    public void logoutAll(UUID userId) {
        revokeAllSessions(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse currentUser(UUID userId) {
        IdentityUser user = users.findById(userId)
                .orElseThrow(() -> new AuthException("UNAUTHORIZED", "The account no longer exists"));
        return toUserResponse(user);
    }

    @Transactional
    public AuthDtos.UserResponse updateProfile(UUID userId, AuthDtos.ProfileRequest request) {
        IdentityUser user = users.findById(userId)
                .orElseThrow(() -> new AuthException("UNAUTHORIZED", "The account no longer exists"));
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().trim());
            user.setUpdatedAt(Instant.now());
            users.save(user);
        }
        IdentityProfile profile = profiles.findByUserId(userId)
                .orElseGet(() -> {
                    IdentityProfile created = new IdentityProfile();
                    created.setId(UUID.randomUUID());
                    created.setUserId(userId);
                    created.setLocale("fr");
                    created.setUpdatedAt(Instant.now());
                    return created;
                });
        if (request.gradeId() != null) profile.setGradeId(request.gradeId());
        if (request.gradeLabel() != null) profile.setGradeLabel(request.gradeLabel());
        if (request.cycle() != null) profile.setCycle(request.cycle());
        if (request.locale() != null) profile.setLocale(normalizeLocale(request.locale()));
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        profile.setUpdatedAt(Instant.now());
        profiles.save(profile);
        return toUserResponse(user);
    }

    public String newRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID();
    }

    public String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private AuthResult issueSession(IdentityUser user) {
        AuthDtos.AuthResponse response = issueAccessOnly(user);
        String refreshToken = newRefreshToken();
        createRefreshSession(user.getId(), refreshToken);
        return new AuthResult(response, refreshToken);
    }

    private AuthDtos.AuthResponse issueAccessOnly(IdentityUser user) {
        String accessToken = jwtService.createAccessToken(
                user.getId(), user.getNormalizedEmail(), user.getRoles());
        TokenRecord tokenRecord = new TokenRecord();
        tokenRecord.setId(UUID.randomUUID());
        tokenRecord.setUserId(user.getId());
        tokenRecord.setAccessTokenHash(sha256(accessToken));
        tokenRecord.setExpiresAt(Instant.now().plusSeconds(jwtService.accessTokenSeconds()));
        tokenRecord.setCreatedAt(Instant.now());
        tokenRecords.save(tokenRecord);

        return new AuthDtos.AuthResponse(
                accessToken, jwtService.accessTokenSeconds(), toUserResponse(user));
    }

    private void createRefreshSession(UUID userId, String refreshToken) {
        Instant now = Instant.now();
        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setTokenHash(sha256(refreshToken));
        session.setStatus(RefreshSession.Status.ACTIVE);
        session.setExpiresAt(now.plusSeconds(refreshTokenDays * 86400));
        session.setCreatedAt(now);
        refreshSessions.save(session);
    }

    private String issueRefreshToken(RefreshSession existing) {
        String replacementToken = newRefreshToken();
        Instant now = Instant.now();
        existing.setStatus(RefreshSession.Status.ROTATED);
        existing.setRevokedAt(now);
        existing.setReplacedByHash(sha256(replacementToken));
        refreshSessions.save(existing);
        createRefreshSession(existing.getUserId(), replacementToken);
        return replacementToken;
    }

    private void revokeAllSessions(UUID userId, Instant now) {
        for (RefreshSession session : refreshSessions.findByUserIdAndStatus(userId, RefreshSession.Status.ACTIVE)) {
            session.setStatus(RefreshSession.Status.REVOKED);
            session.setRevokedAt(now);
            refreshSessions.save(session);
        }
    }

    private AuthDtos.UserResponse toUserResponse(IdentityUser user) {
        IdentityProfile profile = profiles.findByUserId(user.getId()).orElse(null);
        AuthDtos.ProfileResponse profileResponse = profile == null
                ? null
                : new AuthDtos.ProfileResponse(
                        profile.getId(), user.getDisplayName(), profile.getGradeId(),
                        profile.getGradeLabel(), profile.getCycle(), profile.getLocale(),
                        profile.getAvatarUrl());
        return new AuthDtos.UserResponse(
                user.getId(), user.getNormalizedEmail(), user.getDisplayName(),
                user.getRoles(), profileResponse);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeLocale(String locale) {
        return "ar".equalsIgnoreCase(locale) ? "ar" : "fr";
    }
}
