package tn.mathzero.resource.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.resource.domain.LearningContent;

public interface LearningContentRepository extends JpaRepository<LearningContent, UUID> {
    Optional<LearningContent> findByResourceId(UUID resourceId);

    boolean existsByResourceId(UUID resourceId);
}
