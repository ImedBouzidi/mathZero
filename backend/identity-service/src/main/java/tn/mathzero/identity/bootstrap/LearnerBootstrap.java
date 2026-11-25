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

@Component
public class LearnerBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LearnerBootstrap.class);

    private static final LearnerDefinition[] LEARNERS = {
            new LearnerDefinition("primary@mathplay.com", "Primary Learner", "primary"),
            new LearnerDefinition("basic@mathplay.com", "Basic Learner", "basic"),
            new LearnerDefinition("secondary@mathplay.com", "Secondary Learner", "secondary"),
    };

    private final IdentityUserRepository users;
    private final IdentityProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String password;

    public LearnerBootstrap(
            IdentityUserRepository users,
            IdentityProfileRepository profiles,
            PasswordEncoder passwordEncoder,
            @Value("${app.learners.bootstrap.enabled:true}") boolean enabled,
            @Value("${app.learners.bootstrap.password:}") String password) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.debug("Learner bootstrap is disabled");
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("Learner bootstrap skipped: APP_LEARNER_PASSWORD is not configured");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("APP_LEARNER_PASSWORD must contain at least 12 characters");
        }

        Instant now = Instant.now();
        for (LearnerDefinition learner : LEARNERS) {
            String normalizedEmail = learner.email().toLowerCase(Locale.ROOT);
            IdentityUser user = users.findByNormalizedEmail(normalizedEmail).orElse(null);
            if (user == null) {
                user = new IdentityUser();
                user.setId(UUID.randomUUID());
                user.setNormalizedEmail(normalizedEmail);
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setDisplayName(learner.displayName());
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                user.setRoles(new HashSet<>(Set.of(UserRole.STUDENT)));
                users.save(user);
                log.info("Bootstrapped learner {}", normalizedEmail);
            } else {
                if (!user.getRoles().contains(UserRole.STUDENT)) {
                    throw new IllegalStateException(
                            "Configured learner email already belongs to a non-student account");
                }
                boolean changed = false;
                if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode(password));
                    changed = true;
                }
                if (!learner.displayName().equals(user.getDisplayName())) {
                    user.setDisplayName(learner.displayName());
                    changed = true;
                }
                if (changed) {
                    user.setUpdatedAt(now);
                    users.save(user);
                }
            }

            var existingProfile = profiles.findByUserId(user.getId()).orElse(null);
            IdentityProfile profile = existingProfile;
            if (profile == null) {
                profile = new IdentityProfile();
                profile.setId(UUID.randomUUID());
                profile.setUserId(user.getId());
                profile.setLocale("fr");
            }
            if (!learner.cycle().equals(profile.getCycle())) {
                profile.setCycle(learner.cycle());
            }
            profile.setUpdatedAt(now);
            profiles.save(profile);
        }
    }

    private record LearnerDefinition(String email, String displayName, String cycle) {}
}
