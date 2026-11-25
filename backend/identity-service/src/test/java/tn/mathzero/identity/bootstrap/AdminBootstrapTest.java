package tn.mathzero.identity.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import tn.mathzero.identity.domain.IdentityProfile;
import tn.mathzero.identity.domain.IdentityUser;
import tn.mathzero.identity.domain.UserRole;
import tn.mathzero.identity.repository.IdentityProfileRepository;
import tn.mathzero.identity.repository.IdentityUserRepository;

class AdminBootstrapTest {
    private static final String TEST_PASSWORD = "bootstrap-test-password";
    private final IdentityUserRepository users = org.mockito.Mockito.mock(IdentityUserRepository.class);
    private final IdentityProfileRepository profiles =
            org.mockito.Mockito.mock(IdentityProfileRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsTheAdministratorWithTheConfiguredCredentials() {
        when(users.findByNormalizedEmail("admin@mathplay.com")).thenReturn(Optional.empty());
        when(profiles.findByUserId(any())).thenReturn(Optional.empty());

        AdminBootstrap bootstrap = new AdminBootstrap(
                users, profiles, passwordEncoder, true,
                "admin@mathplay.com", TEST_PASSWORD, "Platform Administrator");
        bootstrap.run(null);

        ArgumentCaptor<IdentityUser> userCaptor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(users).save(userCaptor.capture());
        IdentityUser saved = userCaptor.getValue();
        assertEquals("admin@mathplay.com", saved.getNormalizedEmail());
        assertTrue(saved.getRoles().contains(UserRole.ADMIN));
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, saved.getPasswordHash()));
        verify(profiles).save(any(IdentityProfile.class));
    }

    @Test
    void doesNotPromoteAnExistingNonAdminAccount() {
        IdentityUser existing = new IdentityUser();
        existing.setId(UUID.randomUUID());
        existing.setNormalizedEmail("admin@mathplay.com");
        existing.setDisplayName("Existing");
        existing.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        existing.setRoles(Set.of(UserRole.STUDENT));

        when(users.findByNormalizedEmail("admin@mathplay.com")).thenReturn(Optional.of(existing));

        AdminBootstrap bootstrap = new AdminBootstrap(
                users, profiles, passwordEncoder, true,
                "admin@mathplay.com", TEST_PASSWORD, "Platform Administrator");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        assertTrue(existing.getRoles().contains(UserRole.STUDENT));
        verify(users, never()).save(any());
        verify(profiles, never()).save(any(IdentityProfile.class));
    }

    @Test
    void leavesAnExistingAdministratorUnchangedWhenCredentialsMatch() {
        IdentityUser existing = new IdentityUser();
        existing.setId(UUID.randomUUID());
        existing.setNormalizedEmail("admin@mathplay.com");
        existing.setDisplayName("Existing administrator");
        existing.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        existing.setRoles(Set.of(UserRole.ADMIN));

        when(users.findByNormalizedEmail("admin@mathplay.com")).thenReturn(Optional.of(existing));
        when(profiles.findByUserId(existing.getId())).thenReturn(Optional.of(new IdentityProfile()));

        new AdminBootstrap(users, profiles, passwordEncoder, true,
                "admin@mathplay.com", TEST_PASSWORD, "Platform Administrator").run(null);

        verify(users, never()).save(any());
        verify(profiles, never()).save(any());
    }

    @Test
    void skipsBootstrapWhenNoPasswordIsConfigured() {
        new AdminBootstrap(users, profiles, passwordEncoder, true,
                "admin@mathplay.com", "", "Platform Administrator").run(null);

        verify(users, never()).save(any());
        verify(profiles, never()).save(any());
    }

    @Test
    void doesNothingWhenDisabled() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                users, profiles, passwordEncoder, false,
                "admin@mathplay.com", TEST_PASSWORD, "Platform Administrator");
        bootstrap.run(null);

        verify(users, never()).save(any());
    }
}
