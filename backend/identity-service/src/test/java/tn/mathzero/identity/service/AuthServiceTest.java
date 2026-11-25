package tn.mathzero.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import tn.mathzero.identity.domain.IdentityUser;
import tn.mathzero.identity.domain.RefreshSession;
import tn.mathzero.identity.domain.UserRole;
import tn.mathzero.identity.repository.IdentityProfileRepository;
import tn.mathzero.identity.repository.IdentityUserRepository;
import tn.mathzero.identity.repository.RefreshSessionRepository;
import tn.mathzero.identity.repository.TokenRecordRepository;
import tn.mathzero.identity.security.AuthException;
import tn.mathzero.identity.security.JwtService;

class AuthServiceTest {
    private final IdentityUserRepository users = mock(IdentityUserRepository.class);
    private final IdentityProfileRepository profiles = mock(IdentityProfileRepository.class);
    private final RefreshSessionRepository refreshSessions = mock(RefreshSessionRepository.class);
    private final TokenRecordRepository tokenRecords = mock(TokenRecordRepository.class);
    private final JwtService jwtService = mock(JwtService.class);

    private final AuthService authService = new AuthService(
            users,
            profiles,
            refreshSessions,
            tokenRecords,
            new BCryptPasswordEncoder(),
            jwtService,
            30);

    @Test
    void rejectsRefreshReuseAndRevokesSessionFamily() {
        String refreshToken = "previously-used-token";
        RefreshSession reused = refreshSession(refreshToken, RefreshSession.Status.ROTATED);

        when(refreshSessions.findByTokenHash(authService.sha256(refreshToken)))
                .thenReturn(Optional.of(reused));
        when(refreshSessions.findByUserIdAndStatus(reused.getUserId(), RefreshSession.Status.ACTIVE))
                .thenReturn(List.of());

        assertThrows(AuthException.class, () -> authService.refresh(refreshToken));
        verify(refreshSessions).save(reused);
    }

    @Test
    void rotatesAnActiveRefreshSession() {
        String refreshToken = "active-token";
        RefreshSession active = refreshSession(refreshToken, RefreshSession.Status.ACTIVE);
        IdentityUser user = new IdentityUser();
        user.setId(active.getUserId());
        user.setNormalizedEmail("learner@example.com");
        user.setDisplayName("Learner");
        user.setRoles(java.util.Set.of(UserRole.STUDENT));

        when(refreshSessions.findByTokenHash(authService.sha256(refreshToken)))
                .thenReturn(Optional.of(active));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(any(), any(), any())).thenReturn("signed-access-token");
        when(jwtService.accessTokenSeconds()).thenReturn(900L);
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.empty());

        AuthService.AuthResult result = authService.refresh(refreshToken);

        assertNotNull(result.refreshToken());
        assertEquals("signed-access-token", result.response().accessToken());
        verify(refreshSessions, org.mockito.Mockito.atLeastOnce()).save(any(RefreshSession.class));
    }

    private RefreshSession refreshSession(String refreshToken, RefreshSession.Status status) {
        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID());
        session.setUserId(UUID.randomUUID());
        session.setTokenHash(authService.sha256(refreshToken));
        session.setStatus(status);
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        session.setCreatedAt(Instant.now());
        return session;
    }
}
