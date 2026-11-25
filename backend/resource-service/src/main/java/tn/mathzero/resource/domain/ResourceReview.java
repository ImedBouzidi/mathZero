package tn.mathzero.resource.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource_reviews")
public class ResourceReview {
    public enum Decision {
        SUBMIT,
        APPROVE,
        REJECT
    }

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private ExternalResource resource;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Decision decision;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ExternalResource getResource() { return resource; }
    public void setResource(ExternalResource resource) { this.resource = resource; }
    public UUID getReviewerId() { return reviewerId; }
    public void setReviewerId(UUID reviewerId) { this.reviewerId = reviewerId; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}
