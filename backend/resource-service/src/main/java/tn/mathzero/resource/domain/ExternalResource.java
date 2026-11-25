package tn.mathzero.resource.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "external_resources")
public class ExternalResource {
    public enum ReviewStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        REJECTED
    }

    public enum Type {
        ARTICLE,
        AUDIO,
        DOCUMENT,
        IMAGE,
        INTERACTIVE,
        OTHER,
        VIDEO
    }

    public enum Cycle {
        PRIMARY,
        BASIC,
        SECONDARY
    }

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(name = "embed_url", length = 2048)
    private String embedUrl;

    @Column(nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 40)
    private Type resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", length = 20)
    private Cycle cycle;

    @Column(name = "grade_code", length = 80)
    private String gradeCode;

    @Column(name = "subject_code", length = 80)
    private String subjectCode;

    @Column(length = 255)
    private String topic;

    @Column(name = "licence_code", nullable = false, length = 80)
    private String licenceCode;

    @Column(name = "licence_name", nullable = false, length = 180)
    private String licenceName;

    @Column(name = "licence_url", nullable = false, length = 2048)
    private String licenceUrl;

    @Column(name = "attribution_text", nullable = false, columnDefinition = "text")
    private String attributionText;

    @Column(name = "attribution_url", length = 2048)
    private String attributionUrl;

    @Column(name = "retrieved_at")
    private Instant retrievedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Column(name = "review_notes", columnDefinition = "text")
    private String reviewNotes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public String getEmbedUrl() { return embedUrl; }
    public void setEmbedUrl(String embedUrl) { this.embedUrl = embedUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Type getResourceType() { return resourceType; }
    public void setResourceType(Type resourceType) { this.resourceType = resourceType; }
    public Cycle getCycle() { return cycle; }
    public void setCycle(Cycle cycle) { this.cycle = cycle; }
    public String getGradeCode() { return gradeCode; }
    public void setGradeCode(String gradeCode) { this.gradeCode = gradeCode; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getLicenceCode() { return licenceCode; }
    public void setLicenceCode(String licenceCode) { this.licenceCode = licenceCode; }
    public String getLicenceName() { return licenceName; }
    public void setLicenceName(String licenceName) { this.licenceName = licenceName; }
    public String getLicenceUrl() { return licenceUrl; }
    public void setLicenceUrl(String licenceUrl) { this.licenceUrl = licenceUrl; }
    public String getAttributionText() { return attributionText; }
    public void setAttributionText(String attributionText) { this.attributionText = attributionText; }
    public String getAttributionUrl() { return attributionUrl; }
    public void setAttributionUrl(String attributionUrl) { this.attributionUrl = attributionUrl; }
    public Instant getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
