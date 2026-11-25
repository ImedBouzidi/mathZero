package tn.mathzero.resource.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.mathzero.resource.domain.ResourceReview;

public interface ResourceReviewRepository extends JpaRepository<ResourceReview, UUID> {
    List<ResourceReview> findByResource_IdOrderByReviewedAtDesc(UUID resourceId);
}
