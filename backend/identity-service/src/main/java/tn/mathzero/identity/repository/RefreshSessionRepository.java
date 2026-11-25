package tn.mathzero.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.identity.domain.RefreshSession;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findByUserIdAndStatus(UUID userId, RefreshSession.Status status);
}
