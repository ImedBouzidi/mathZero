package tn.mathzero.resource.service;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.mathzero.resource.api.ResourceDtos;
import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.domain.ResourceReview;
import tn.mathzero.resource.repository.ExternalResourceRepository;
import tn.mathzero.resource.repository.ResourceReviewRepository;

@Service
public class ResourceService {
    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final ExternalResourceRepository resources;
    private final ResourceReviewRepository reviews;
    private final List<ResourceProviderAdapter> providers;
    private final LearningContentService learningContent;

    public ResourceService(
            ExternalResourceRepository resources,
            ResourceReviewRepository reviews,
            List<ResourceProviderAdapter> providers,
            LearningContentService learningContent) {
        this.resources = resources;
        this.reviews = reviews;
        this.providers = providers;
        this.learningContent = learningContent;
    }

    @Transactional(readOnly = true)
    public ResourceDtos.Page<ResourceDtos.ResourceView> findApproved(
            String search, String language, String gradeCode, String subjectCode,
            ExternalResource.Cycle cycle, Pageable pageable) {
        Specification<ExternalResource> specification = (root, query, builder) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(builder.equal(root.get("reviewStatus"), ExternalResource.ReviewStatus.APPROVED));
            if (cycle != null) {
                predicates.add(builder.equal(root.get("cycle"), cycle));
            }
            addEquals(predicates, builder, root.get("language"), language);
            addEquals(predicates, builder, root.get("gradeCode"), gradeCode);
            addEquals(predicates, builder, root.get("subjectCode"), subjectCode);
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("topic")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return page(resources.findAll(specification, pageable));
    }

    @Transactional(readOnly = true)
    public ResourceDtos.Page<ResourceDtos.AdminResourceView> findForAdmin(
            ExternalResource.ReviewStatus status, Pageable pageable) {
        Page<ExternalResource> page = status == null
                ? resources.findAll(pageable)
                : resources.findAll((root, query, builder) ->
                        builder.equal(root.get("reviewStatus"), status), pageable);
        return new ResourceDtos.Page<>(
                page.getContent().stream().map(ResourceDtos.AdminResourceView::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * Turns a search result into a draft in the catalog. The selected result is
     * re-resolved through the provider so the client cannot inject titles, URLs
     * or rights metadata. Nothing here publishes: the usual review flow still
     * applies before the resource becomes publicly visible.
     */
    @Transactional
    public ResourceDtos.AdminResourceView importProviderResult(
            UUID actorId, ResourceDtos.ImportProviderResourceRequest request) {
        String providerName = request.provider().name();
        ResourceProviderAdapter adapter = providers.stream()
                .filter(item -> providerName.equalsIgnoreCase(item.descriptor().provider()))
                .findFirst()
                .orElseThrow(() -> ResourceException.invalid("Resource provider is not configured"));

        String language = request.language().trim().toLowerCase(java.util.Locale.ROOT);
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isEmpty()) {
            throw ResourceException.invalid("Search query is required");
        }

        String requestedUrl = request.resultUrl() == null ? "" : request.resultUrl().trim();
        ResourceDtos.ProviderSearchResult candidate = searchProviders(query, 20, language).stream()
                .filter(item -> item.url().equals(requestedUrl))
                .filter(item -> providerName.equalsIgnoreCase(item.source()))
                .findFirst()
                .orElseThrow(() -> ResourceException.invalid(
                        "The selected result is no longer available; search again"));
        if (!adapter.acceptsUrl(candidate.url(), language)) {
            throw ResourceException.invalid("Result URL does not match the selected provider or language");
        }
        ResourceProviderAdapter.ProviderSearchResult adapterCandidate = adapterResult(candidate);
        ResourceProviderAdapter.ProviderRights rights = adapter.rightsFor(adapterCandidate);
        if (rights == null) {
            throw ResourceException.invalid("Selected provider does not support resource import");
        }

        ExternalResource template = buildResource(
                actorId,
                providerName,
                adapter.externalIdFor(adapterCandidate),
                query,
                adapter.resourceLanguage(language),
                request.cycle(),
                candidate,
                rights,
                Instant.now());
        return resources.findByUrl(candidate.url())
                .map(ResourceDtos.AdminResourceView::from)
                .orElseGet(() -> ResourceDtos.AdminResourceView.from(resources.save(template)));
    }

    /**
     * Stores an administrator-approved suggestion as a published resource. The
     * click on "Approve" is the review decision and is recorded in the audit log.
     * Rights, language and external ID always come from the owning adapter.
     */
    @Transactional
    public ResourceDtos.AdminResourceView createApprovedFromSuggestion(
            UUID actorId,
            ResourceDtos.Provider provider,
            String query,
            String language,
            ExternalResource.Cycle cycle,
            ResourceDtos.ProviderSearchResult candidate) {
        if (provider == null) {
            throw ResourceException.invalid("Suggestion provider is required");
        }
        ResourceProviderAdapter adapter = providers.stream()
                .filter(item -> provider.name().equalsIgnoreCase(item.descriptor().provider()))
                .findFirst()
                .orElseThrow(() -> ResourceException.invalid("Resource provider is not configured"));
        ResourceProviderAdapter.ProviderSearchResult adapterCandidate = adapterResult(candidate);
        if (!adapter.acceptsUrl(adapterCandidate.url(), language)) {
            throw ResourceException.invalid("Result URL does not match the selected provider or language");
        }
        ResourceProviderAdapter.ProviderRights rights = adapter.rightsFor(adapterCandidate);
        if (rights == null) {
            throw ResourceException.invalid("Selected provider does not support resource approval");
        }

        Instant now = Instant.now();
        ExternalResource template = buildResource(
                actorId,
                provider.name(),
                adapter.externalIdFor(adapterCandidate),
                query,
                adapter.resourceLanguage(language),
                cycle,
                candidate,
                rights,
                now);
        ExternalResource resource = resources.findByUrl(candidate.url()).orElse(template);
        if (resource != template) {
            // An earlier draft may exist for this URL (e.g. from provider import).
            // Publish that same record so approval cannot leave a stale draft behind.
            applyProviderMetadata(resource, template);
        }
        if (resource.getReviewStatus() == ExternalResource.ReviewStatus.APPROVED) {
            return ResourceDtos.AdminResourceView.from(resource);
        }

        resource.setReviewStatus(ExternalResource.ReviewStatus.APPROVED);
        resource.setReviewNotes("Approved from " + provider.name() + " suggestions");
        resource.setVerifiedAt(now);
        resource.setUpdatedAt(now);
        ExternalResource saved = resources.save(resource);

        ResourceReview review = new ResourceReview();
        review.setId(UUID.randomUUID());
        review.setResource(saved);
        review.setReviewerId(actorId);
        review.setDecision(ResourceReview.Decision.APPROVE);
        review.setNotes(resource.getReviewNotes());
        review.setReviewedAt(now);
        reviews.save(review);
        learningContent.ensureFor(saved.getId());
        return ResourceDtos.AdminResourceView.from(saved);
    }

    private static void applyProviderMetadata(ExternalResource target, ExternalResource source) {
        target.setProvider(source.getProvider());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setCanonicalUrl(source.getCanonicalUrl());
        target.setExternalId(source.getExternalId());
        target.setLanguage(source.getLanguage());
        target.setResourceType(source.getResourceType());
        target.setCycle(source.getCycle());
        target.setTopic(source.getTopic());
        target.setLicenceCode(source.getLicenceCode());
        target.setLicenceName(source.getLicenceName());
        target.setLicenceUrl(source.getLicenceUrl());
        target.setAttributionText(source.getAttributionText());
        target.setAttributionUrl(source.getAttributionUrl());
        target.setRetrievedAt(source.getRetrievedAt());
    }

    private static ExternalResource buildResource(
            UUID actorId,
            String provider,
            String externalId,
            String query,
            String language,
            ExternalResource.Cycle cycle,
            ResourceDtos.ProviderSearchResult candidate,
            ResourceProviderAdapter.ProviderRights rights,
            Instant now) {
        ExternalResource resource = new ExternalResource();
        resource.setId(UUID.randomUUID());
        resource.setProvider(provider);
        resource.setTitle(capped(candidate.title(), 500));
        resource.setDescription(trimToNull(candidate.snippet()));
        resource.setUrl(capped(candidate.url(), 2048));
        resource.setCanonicalUrl(capped(candidate.url(), 2048));
        resource.setExternalId(capped(externalId == null ? candidate.url() : externalId, 255));
        resource.setLanguage(language);
        resource.setResourceType(rights.type());
        resource.setCycle(cycle);
        resource.setTopic(capped(query, 255));
        resource.setLicenceCode(capped(rights.licenceCode(), 80));
        resource.setLicenceName(capped(rights.licenceName(), 180));
        resource.setLicenceUrl(capped(rights.licenceUrl(), 2048));
        resource.setAttributionText(rights.attributionText());
        resource.setAttributionUrl(capped(rights.attributionUrl(), 2048));
        resource.setRetrievedAt(now);
        resource.setReviewStatus(ExternalResource.ReviewStatus.DRAFT);
        resource.setCreatedBy(actorId);
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        return resource;
    }

    private static ResourceProviderAdapter.ProviderSearchResult adapterResult(
            ResourceDtos.ProviderSearchResult item) {
        return new ResourceProviderAdapter.ProviderSearchResult(
                item.title(), item.url(), item.snippet(), item.source());
    }

    private static String capped(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Transactional
    public ResourceDtos.AdminResourceView createManual(UUID actorId, ResourceDtos.CreateManualRequest request) {
        requireHttps(request.url(), "url");
        requireHttps(request.licenceUrl(), "licenceUrl");
        if (request.attributionUrl() != null && !request.attributionUrl().isBlank()) {
            requireHttps(request.attributionUrl(), "attributionUrl");
        }

        Instant now = Instant.now();
        ExternalResource resource = new ExternalResource();
        resource.setId(UUID.randomUUID());
        resource.setProvider("MANUAL");
        resource.setTitle(request.title().trim());
        resource.setDescription(trimToNull(request.description()));
        resource.setUrl(request.url().trim());
        resource.setCanonicalUrl(request.url().trim());
        resource.setLanguage(request.language().trim().toLowerCase(java.util.Locale.ROOT));
        resource.setResourceType(request.resourceType());
        resource.setCycle(request.cycle());
        resource.setGradeCode(trimToNull(request.gradeCode()));
        resource.setSubjectCode(trimToNull(request.subjectCode()));
        resource.setTopic(trimToNull(request.topic()));
        resource.setLicenceCode(request.licenceCode().trim());
        resource.setLicenceName(request.licenceName().trim());
        resource.setLicenceUrl(request.licenceUrl().trim());
        resource.setAttributionText(request.attributionText().trim());
        resource.setAttributionUrl(trimToNull(request.attributionUrl()));
        resource.setReviewStatus(ExternalResource.ReviewStatus.DRAFT);
        resource.setCreatedBy(actorId);
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        return ResourceDtos.AdminResourceView.from(resources.save(resource));
    }

    @Transactional
    public ResourceDtos.AdminResourceView review(
            UUID reviewerId, UUID resourceId, ResourceDtos.ReviewRequest request) {
        ExternalResource resource = resources.findById(resourceId).orElseThrow(ResourceException::notFound);
        ExternalResource.ReviewStatus nextStatus = switch (request.action()) {
            case SUBMIT -> {
                if (resource.getReviewStatus() != ExternalResource.ReviewStatus.DRAFT
                        && resource.getReviewStatus() != ExternalResource.ReviewStatus.REJECTED) {
                    throw ResourceException.conflict("Only draft or rejected resources can be submitted");
                }
                yield ExternalResource.ReviewStatus.SUBMITTED;
            }
            case APPROVE -> {
                if (resource.getReviewStatus() != ExternalResource.ReviewStatus.SUBMITTED) {
                    throw ResourceException.conflict("Only submitted resources can be approved");
                }
                yield ExternalResource.ReviewStatus.APPROVED;
            }
            case REJECT -> {
                if (resource.getReviewStatus() != ExternalResource.ReviewStatus.SUBMITTED) {
                    throw ResourceException.conflict("Only submitted resources can be rejected");
                }
                yield ExternalResource.ReviewStatus.REJECTED;
            }
        };

        Instant now = Instant.now();
        resource.setReviewStatus(nextStatus);
        resource.setReviewNotes(trimToNull(request.notes()));
        resource.setUpdatedAt(now);
        if (nextStatus == ExternalResource.ReviewStatus.APPROVED) {
            resource.setVerifiedAt(now);
        }
        resources.save(resource);

        ResourceReview review = new ResourceReview();
        review.setId(UUID.randomUUID());
        review.setResource(resource);
        review.setReviewerId(reviewerId);
        review.setDecision(ResourceReview.Decision.valueOf(request.action().name()));
        review.setNotes(trimToNull(request.notes()));
        review.setReviewedAt(now);
        reviews.save(review);
        if (nextStatus == ExternalResource.ReviewStatus.APPROVED) {
            learningContent.ensureFor(resourceId);
        }
        return ResourceDtos.AdminResourceView.from(resource);
    }

    @Transactional(readOnly = true)
    public List<ResourceDtos.ReviewView> reviewHistory(UUID resourceId) {
        if (!resources.existsById(resourceId)) {
            throw ResourceException.notFound();
        }
        return reviews.findByResource_IdOrderByReviewedAtDesc(resourceId).stream()
                .map(ResourceDtos.ReviewView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceProviderDescriptor> providerStatus() {
        return providers.stream()
                .map(ResourceProviderAdapter::descriptor)
                .sorted(Comparator.comparing(ResourceProviderDescriptor::provider))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceDtos.ProviderSearchResult> searchProviders(
            String query, int limit, String language) {
        List<ResourceDtos.ProviderSearchResult> results = new java.util.ArrayList<>();
        for (ResourceProviderAdapter provider : providers) {
            // Each catalogue has its own language rules (Wikimedia accepts only
            // fr/ar, OpenStax only en). One provider rejecting a language must
            // not fail the generic search or the whole suggestion board.
            try {
                for (ResourceProviderAdapter.ProviderSearchResult item
                        : provider.search(query, limit, language)) {
                    results.add(new ResourceDtos.ProviderSearchResult(
                            item.title(), item.url(), item.snippet(), item.source()));
                }
            } catch (RuntimeException exception) {
                log.debug(
                        "Provider {} skipped query '{}' in language {}: {}",
                        provider.descriptor().provider(),
                        query,
                        language,
                        exception.getMessage());
            }
        }
        return results;
    }

    /**
     * Searches a single provider. The suggestion board configures queries per
     * provider and language, so it must not fan a query out to every adapter.
     */
    @Transactional(readOnly = true)
    public List<ResourceDtos.ProviderSearchResult> searchProvider(
            ResourceDtos.Provider provider, String query, int limit, String language) {
        if (provider == null) {
            return List.of();
        }
        ResourceProviderAdapter adapter = providers.stream()
                .filter(item -> provider.name().equalsIgnoreCase(item.descriptor().provider()))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            return List.of();
        }
        try {
            return adapter.search(query, limit, language).stream()
                    .map(item -> new ResourceDtos.ProviderSearchResult(
                            item.title(), item.url(), item.snippet(), item.source()))
                    .toList();
        } catch (RuntimeException exception) {
            log.debug(
                    "Provider {} skipped query '{}' in language {}: {}",
                    provider,
                    query,
                    language,
                    exception.getMessage());
            return List.of();
        }
    }

    private static ResourceDtos.Page<ResourceDtos.ResourceView> page(Page<ExternalResource> source) {
        return new ResourceDtos.Page<>(
                source.getContent().stream().map(ResourceDtos.ResourceView::from).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }

    private static void addEquals(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.Path<String> path,
            String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(builder.equal(builder.lower(path), value.trim().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private static void requireHttps(String value, String field) {
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw ResourceException.invalid(field + " must be an absolute HTTPS URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw ResourceException.invalid(field + " must be an absolute HTTPS URL");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
