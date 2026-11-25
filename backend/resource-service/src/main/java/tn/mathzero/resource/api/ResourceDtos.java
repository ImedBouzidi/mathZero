package tn.mathzero.resource.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.domain.ResourceReview;
import tn.mathzero.resource.service.ResourceProviderDescriptor;

public final class ResourceDtos {
    private ResourceDtos() {}

    public record ApiResponse<T>(T data, Meta meta) {}
    public record Meta(String requestId) {}
    public record ApiError(String code, String message, String requestId) {}

    public record Page<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}

    /** External catalogues that can contribute review candidates. */
    public enum Provider {
        WIKIMEDIA,
        OPENSTAX,
        GEOGEBRA,
        PHET
    }

    public record CreateManualRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank @Size(max = 2048) String url,
            @Size(max = 4000) String description,
            @NotBlank @Pattern(regexp = "(?i)fr|ar") String language,
            @NotNull ExternalResource.Type resourceType,
            @NotNull ExternalResource.Cycle cycle,
            @Size(max = 80) String gradeCode,
            @Size(max = 80) String subjectCode,
            @Size(max = 255) String topic,
            @NotBlank @Size(max = 80) String licenceCode,
            @NotBlank @Size(max = 180) String licenceName,
            @NotBlank @Size(max = 2048) String licenceUrl,
            @NotBlank @Size(max = 4000) String attributionText,
            @Size(max = 2048) String attributionUrl) {}

    /**
     * Reference to a result from an earlier provider search. The server looks the
     * result up again instead of trusting client-supplied titles or license data.
     */
    public record ImportProviderResourceRequest(
            @NotNull Provider provider,
            @NotBlank @Size(max = 200) String query,
            @NotBlank @Size(max = 2048) String resultUrl,
            @NotBlank @Pattern(regexp = "(?i)fr|ar") String language,
            @NotNull ExternalResource.Cycle cycle) {}

    public enum ReviewAction {
        SUBMIT,
        APPROVE,
        REJECT
    }

    public record ReviewRequest(@NotNull ReviewAction action, @Size(max = 4000) String notes) {}

    public record ResourceView(
            UUID id,
            String title,
            String description,
            String url,
            String language,
            ExternalResource.Type resourceType,
            ExternalResource.Cycle cycle,
            String gradeCode,
            String subjectCode,
            String topic,
            String licenceCode,
            String licenceName,
            String licenceUrl,
            String attributionText,
            String attributionUrl) {
        public static ResourceView from(ExternalResource resource) {
            return new ResourceView(
                    resource.getId(), resource.getTitle(), resource.getDescription(), resource.getUrl(),
                    resource.getLanguage(), resource.getResourceType(), resource.getCycle(),
                    resource.getGradeCode(), resource.getSubjectCode(), resource.getTopic(),
                    resource.getLicenceCode(), resource.getLicenceName(), resource.getLicenceUrl(),
                    resource.getAttributionText(), resource.getAttributionUrl());
        }
    }

    public record AdminResourceView(
            UUID id,
            String provider,
            String title,
            String description,
            String url,
            String language,
            ExternalResource.Type resourceType,
            ExternalResource.Cycle cycle,
            String gradeCode,
            String subjectCode,
            String topic,
            String licenceCode,
            String licenceName,
            String licenceUrl,
            String attributionText,
            String attributionUrl,
            ExternalResource.ReviewStatus reviewStatus,
            String reviewNotes,
            UUID createdBy,
            Instant verifiedAt,
            Instant createdAt,
            Instant updatedAt) {
        public static AdminResourceView from(ExternalResource resource) {
            return new AdminResourceView(
                    resource.getId(), resource.getProvider(), resource.getTitle(), resource.getDescription(),
                    resource.getUrl(), resource.getLanguage(), resource.getResourceType(), resource.getCycle(),
                    resource.getGradeCode(), resource.getSubjectCode(), resource.getTopic(),
                    resource.getLicenceCode(), resource.getLicenceName(), resource.getLicenceUrl(),
                    resource.getAttributionText(), resource.getAttributionUrl(), resource.getReviewStatus(),
                    resource.getReviewNotes(), resource.getCreatedBy(), resource.getVerifiedAt(),
                    resource.getCreatedAt(), resource.getUpdatedAt());
        }
    }

    public record ReviewView(
            UUID id,
            UUID reviewerId,
            ResourceReview.Decision decision,
            String notes,
            Instant reviewedAt) {
        public static ReviewView from(ResourceReview review) {
            return new ReviewView(
                    review.getId(), review.getReviewerId(), review.getDecision(),
                    review.getNotes(), review.getReviewedAt());
        }
    }

    public record ProviderList(List<ResourceProviderDescriptor> providers) {}

    public record ProviderSearchResult(String title, String url, String snippet, String source) {}

    /** A provider candidate pre-sorted into one of the three experience cycles. */
    public record Suggestion(
            Provider provider,
            ExternalResource.Cycle cycle,
            String language,
            String query,
            String title,
            String url,
            String snippet,
            String source) {}

    public record SuggestionList(List<Suggestion> items) {}

    public record ApproveSuggestionRequest(
            @NotNull Provider provider,
            @NotNull ExternalResource.Cycle cycle,
            @NotBlank @Pattern(regexp = "(?i)fr|ar|en") String language,
            @NotBlank @Size(max = 200) String query,
            @NotBlank @Size(max = 2048) String resultUrl) {}
}
