package tn.mathzero.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.identity.domain.IdentityProfile;

public interface IdentityProfileRepository extends JpaRepository<IdentityProfile, UUID> {
    Optional<IdentityProfile> findByUserId(UUID userId);
}
