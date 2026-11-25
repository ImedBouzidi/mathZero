package tn.mathzero.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.identity.domain.IdentityUser;

public interface IdentityUserRepository extends JpaRepository<IdentityUser, UUID> {
    Optional<IdentityUser> findByNormalizedEmail(String normalizedEmail);
    boolean existsByNormalizedEmail(String normalizedEmail);
}
