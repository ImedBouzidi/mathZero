package tn.mathzero.identity.bootstrap;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tn.mathzero.identity.domain.IdentityProfile;
import tn.mathzero.identity.domain.IdentityUser;
import tn.mathzero.identity.domain.UserRole;
import tn.mathzero.identity.repository.IdentityProfileRepository;
import tn.mathzero.identity.repository.IdentityUserRepository;

/**
 * Creates or promotes the first platform administrator at startup.
 *
 * <p>Registration always assigns {@link UserRole#STUDENT}; this bootstrap is the
 * only supported path that grants {@link UserRole#ADMIN}. It is idempotent and
 * can be disabled with {@code APP_ADMIN_BOOTSTRAP_ENABLED=false} in any
 * environment where administrators are managed out of band.</p>
 *
 * <p>The password is supplied out of band through {@code APP_ADMIN_PASSWORD};
 * it is never stored in application configuration. Disable bootstrap after
 * the first administrator exists in production.</p>
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final IdentityUserRepository users;
    private final IdentityProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminBootstrap(
            IdentityUserRepository users,
            IdentityProfileRepository profiles,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.enabled:true}") boolean enabled,
            @Value("${app.admin.bootstrap.email:admin@mathplay.com}") String email,
            @Value("${app.admin.bootstrap.password:}") String password,
            @Value("${app.admin.bootstrap.display-name:Platform Administrator}") String displayName) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.debug("Administrator bootstrap is disabled");
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("Administrator bootstrap skipped: APP_ADMIN_PASSWORD is not configured");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD must contain at least 12 characters");
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new IllegalStateException("APP_ADMIN_EMAIL must be configured");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalStateException("APP_ADMIN_DISPLAY_NAME must not be blank");
        }

        Instant now = Instant.now();
        IdentityUser user = users.findByNormalizedEmail(normalizedEmail).orElse(null);
        boolean created = false;

        if (user == null) {
            user = new IdentityUser();
            user.setId(UUID.randomUUID());
            user.setNormalizedEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setDisplayName(displayName.trim());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user.setRoles(new HashSet<>(Set.of(UserRole.ADMIN)));
            users.save(user);
            created = true;
        } else {
            if (!user.getRoles().contains(UserRole.ADMIN)) {
                throw new IllegalStateException(
                        "Configured administrator email already belongs to a non-admin account");
            }
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setUpdatedAt(now);
                users.save(user);
            }
        }

        if (profiles.findByUserId(user.getId()).isEmpty()) {
            IdentityProfile profile = new IdentityProfile();
            profile.setId(UUID.randomUUID());
            profile.setUserId(user.getId());
            profile.setLocale("fr");
            profile.setUpdatedAt(now);
            profiles.save(profile);
        }

        if (created) {
            log.info("Bootstrapped platform administrator {}", normalizedEmail);
        } else {
            log.info("Verified platform administrator {}", normalizedEmail);
        }
    }

    private static String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
