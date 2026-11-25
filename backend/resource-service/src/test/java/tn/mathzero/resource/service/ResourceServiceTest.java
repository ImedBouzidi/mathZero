package tn.mathzero.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.mathzero.resource.api.ResourceDtos;
import tn.mathzero.resource.domain.ExternalResource;
import tn.mathzero.resource.domain.ResourceReview;
import tn.mathzero.resource.repository.ExternalResourceRepository;
import tn.mathzero.resource.repository.ResourceReviewRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {
    @Mock private ExternalResourceRepository resources;
    @Mock private ResourceReviewRepository reviews;
    @Mock private LearningContentService learningContent;

    private ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(resources, reviews, List.of(), learningContent);
    }

    private ResourceService withWikimediaAdapter() {
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public ProviderRights rightsFor(ProviderSearchResult result) {
                return WikimediaAdapter.wikimediaRights(result);
            }

            @Override
            public boolean acceptsUrl(String url, String language) {
                return url != null && url.contains("wikipedia.org/wiki/");
            }

            @Override
            public String externalIdFor(ProviderSearchResult result) {
                return result.title();
            }
        };
        return new ResourceService(resources, reviews, List.of(provider), learningContent);
    }

    @Test
    void manuallyCuratedResourcesStartAsDraftWithTheirRightsMetadata() {
        when(resources.save(any(ExternalResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ResourceDtos.CreateManualRequest request = new ResourceDtos.CreateManualRequest(
                "Fractions overview", "https://example.org/fractions", "A reviewed explanation.",
                "fr", ExternalResource.Type.ARTICLE, ExternalResource.Cycle.BASIC, "B7", "MATH", "fractions",
                "CC-BY-4.0", "Creative Commons Attribution 4.0",
                "https://creativecommons.org/licenses/by/4.0/",
                "Author, CC BY 4.0", "https://example.org/author");

        ResourceDtos.AdminResourceView result = service.createManual(UUID.randomUUID(), request);

        assertEquals("MANUAL", result.provider());
        assertEquals(ExternalResource.ReviewStatus.DRAFT, result.reviewStatus());
        assertEquals(ExternalResource.Cycle.BASIC, result.cycle());
        assertEquals("CC-BY-4.0", result.licenceCode());
    }

    @Test
    void rejectsNonHttpsResourceLinks() {
        ResourceDtos.CreateManualRequest request = new ResourceDtos.CreateManualRequest(
                "Fractions overview", "http://example.org/fractions", null,
                "fr", ExternalResource.Type.ARTICLE, ExternalResource.Cycle.PRIMARY, null, null, null,
                "CC-BY-4.0", "Creative Commons Attribution 4.0",
                "https://creativecommons.org/licenses/by/4.0/",
                "Author, CC BY 4.0", null);

        assertThrows(ResourceException.class, () -> service.createManual(UUID.randomUUID(), request));
        verify(resources, never()).save(any(ExternalResource.class));
    }

    @Test
    void approvalRequiresSubmissionAndWritesAnAuditRecord() {
        UUID reviewerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        ExternalResource resource = new ExternalResource();
        resource.setId(resourceId);
        resource.setReviewStatus(ExternalResource.ReviewStatus.SUBMITTED);
        when(resources.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resources.save(resource)).thenReturn(resource);
        when(reviews.save(any(ResourceReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceDtos.AdminResourceView result = service.review(
                reviewerId, resourceId,
                new ResourceDtos.ReviewRequest(ResourceDtos.ReviewAction.APPROVE, "Rights checked"));

        assertEquals(ExternalResource.ReviewStatus.APPROVED, result.reviewStatus());
        assertNotNull(resource.getVerifiedAt());
        ArgumentCaptor<ResourceReview> auditCaptor = ArgumentCaptor.forClass(ResourceReview.class);
        verify(reviews).save(auditCaptor.capture());
        assertEquals(reviewerId, auditCaptor.getValue().getReviewerId());
        assertEquals(ResourceReview.Decision.APPROVE, auditCaptor.getValue().getDecision());
    }

    @Test
    void cannotApproveADraftResource() {
        UUID resourceId = UUID.randomUUID();
        ExternalResource resource = new ExternalResource();
        resource.setId(resourceId);
        resource.setReviewStatus(ExternalResource.ReviewStatus.DRAFT);
        when(resources.findById(resourceId)).thenReturn(Optional.of(resource));

        assertThrows(ResourceException.class, () -> service.review(
                UUID.randomUUID(), resourceId,
                new ResourceDtos.ReviewRequest(ResourceDtos.ReviewAction.APPROVE, null)));

        verify(resources, never()).save(any(ExternalResource.class));
        verify(reviews, never()).save(any(ResourceReview.class));
    }

    @Test
    void importsAWikimediaResultAsADraftWithLicenseAndAttribution() {
        UUID actorId = UUID.randomUUID();
        String url = "https://fr.wikipedia.org/wiki/Fractions";
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                assertEquals("fr", language);
                return List.of(new ProviderSearchResult("Fractions", url, "Aperçu", "Wikimedia"));
            }

            @Override
            public ProviderRights rightsFor(ProviderSearchResult result) {
                return WikimediaAdapter.wikimediaRights(result);
            }

            @Override
            public String externalIdFor(ProviderSearchResult result) {
                return result.title();
            }
        };
        ResourceService providerService = new ResourceService(resources, reviews, List.of(provider), learningContent);
        when(resources.findByUrl(url)).thenReturn(Optional.empty());
        when(resources.save(any(ExternalResource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceDtos.AdminResourceView result = providerService.importProviderResult(actorId,
                new ResourceDtos.ImportProviderResourceRequest(
                        ResourceDtos.Provider.WIKIMEDIA,
                        "fractions", url, "fr", ExternalResource.Cycle.BASIC));

        assertEquals("WIKIMEDIA", result.provider());
        assertEquals(ExternalResource.ReviewStatus.DRAFT, result.reviewStatus());
        assertEquals(ExternalResource.Cycle.BASIC, result.cycle());
        assertEquals("CC-BY-SA-4.0", result.licenceCode());
        assertTrue(result.attributionText().contains("Wikipedia contributors"));
        ArgumentCaptor<ExternalResource> resourceCaptor = ArgumentCaptor.forClass(ExternalResource.class);
        verify(resources).save(resourceCaptor.capture());
        assertEquals(actorId, resourceCaptor.getValue().getCreatedBy());
        verify(reviews, never()).save(any(ResourceReview.class));
    }

    @Test
    void refusesToImportAUrlThatTheProviderDidNotReturn() {
        String url = "https://fr.wikipedia.org/wiki/Fractions";
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult("Fractions", url, "Aperçu", "Wikimedia"));
            }
        };
        ResourceService providerService = new ResourceService(resources, reviews, List.of(provider), learningContent);

        assertThrows(ResourceException.class, () -> providerService.importProviderResult(
                UUID.randomUUID(),
                new ResourceDtos.ImportProviderResourceRequest(
                        ResourceDtos.Provider.WIKIMEDIA,
                        "fractions", "https://fr.wikipedia.org/wiki/Other", "fr",
                        ExternalResource.Cycle.BASIC)));

        verify(resources, never()).save(any(ExternalResource.class));
    }

    @Test
    void approvedSuggestionIsStoredAsApprovedWithAnAuditRecord() {
        UUID actorId = UUID.randomUUID();
        String url = "https://fr.wikipedia.org/wiki/Fractions";
        when(resources.findByUrl(url)).thenReturn(Optional.empty());
        when(resources.save(any(ExternalResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviews.save(any(ResourceReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceDtos.AdminResourceView result = withWikimediaAdapter().createApprovedFromSuggestion(
                actorId, ResourceDtos.Provider.WIKIMEDIA, "fractions", "fr",
                ExternalResource.Cycle.PRIMARY,
                new ResourceDtos.ProviderSearchResult("Fractions", url, "Aperçu", "Wikimedia"));

        assertEquals(ExternalResource.ReviewStatus.APPROVED, result.reviewStatus());
        assertEquals(ExternalResource.Cycle.PRIMARY, result.cycle());
        assertEquals("CC-BY-SA-4.0", result.licenceCode());
        assertNotNull(result.verifiedAt());
        ArgumentCaptor<ResourceReview> auditCaptor = ArgumentCaptor.forClass(ResourceReview.class);
        verify(reviews).save(auditCaptor.capture());
        assertEquals(ResourceReview.Decision.APPROVE, auditCaptor.getValue().getDecision());
        assertEquals(actorId, auditCaptor.getValue().getReviewerId());
        verify(learningContent).ensureFor(result.id());
    }

    @Test
    void approvingAnOpenStaxSuggestionUsesTheAdapterRightsAndLanguage() {
        UUID actorId = UUID.randomUUID();
        String url = "https://openstax.org/details/books/algebra-and-trigonometry";
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("OPENSTAX", "ACTIVE", true, "");
            }

            @Override
            public ProviderRights rightsFor(ProviderSearchResult result) {
                return new ProviderRights(
                        ExternalResource.Type.DOCUMENT,
                        "CC-BY-4.0",
                        "Creative Commons Attribution 4.0",
                        "https://creativecommons.org/licenses/by/4.0/",
                        "OpenStax, " + result.title() + " (CC BY 4.0)",
                        result.url());
            }

            @Override
            public boolean acceptsUrl(String value, String language) {
                return value != null && value.contains("/details/books/");
            }

            @Override
            public String externalIdFor(ProviderSearchResult result) {
                return "algebra-and-trigonometry";
            }

            @Override
            public String resourceLanguage(String selectedLanguage) {
                return "en";
            }
        };
        ResourceService providerService =
                new ResourceService(resources, reviews, List.of(provider), learningContent);
        when(resources.findByUrl(url)).thenReturn(Optional.empty());
        when(resources.save(any(ExternalResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviews.save(any(ResourceReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceDtos.AdminResourceView result = providerService.createApprovedFromSuggestion(
                actorId, ResourceDtos.Provider.OPENSTAX, "functions", "en",
                ExternalResource.Cycle.SECONDARY,
                new ResourceDtos.ProviderSearchResult(
                        "Algebra and Trigonometry", url, "English textbook", "OPENSTAX"));

        assertEquals("OPENSTAX", result.provider());
        assertEquals("en", result.language());
        assertEquals("CC-BY-4.0", result.licenceCode());
        assertEquals("Approved from OPENSTAX suggestions", result.reviewNotes());
        verify(learningContent).ensureFor(result.id());
    }

    @Test
    void approvingASuggestionPublishesAnExistingDraftForTheSameUrl() {
        UUID actorId = UUID.randomUUID();
        String url = "https://fr.wikipedia.org/wiki/Fractions";
        ExternalResource draft = new ExternalResource();
        draft.setId(UUID.randomUUID());
        draft.setUrl(url);
        draft.setCycle(ExternalResource.Cycle.SECONDARY);
        draft.setReviewStatus(ExternalResource.ReviewStatus.DRAFT);
        when(resources.findByUrl(url)).thenReturn(Optional.of(draft));
        when(resources.save(any(ExternalResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviews.save(any(ResourceReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceDtos.AdminResourceView result = withWikimediaAdapter().createApprovedFromSuggestion(
                actorId, ResourceDtos.Provider.WIKIMEDIA, "fractions", "fr",
                ExternalResource.Cycle.PRIMARY,
                new ResourceDtos.ProviderSearchResult("Fractions", url, "Aperçu", "Wikimedia"));

        assertEquals(draft.getId(), result.id());
        assertEquals(ExternalResource.ReviewStatus.APPROVED, result.reviewStatus());
        assertEquals(ExternalResource.Cycle.PRIMARY, result.cycle());
        assertNotNull(result.verifiedAt());
        verify(resources).save(draft);
        verify(reviews).save(any(ResourceReview.class));
    }

    @Test
    void approvingAnAlreadyApprovedUrlDoesNotCreateAnotherAuditRecord() {
        UUID actorId = UUID.randomUUID();
        String url = "https://fr.wikipedia.org/wiki/Fractions";
        ExternalResource existing = new ExternalResource();
        existing.setId(UUID.randomUUID());
        existing.setUrl(url);
        existing.setReviewStatus(ExternalResource.ReviewStatus.APPROVED);
        when(resources.findByUrl(url)).thenReturn(Optional.of(existing));

        ResourceDtos.AdminResourceView result = withWikimediaAdapter().createApprovedFromSuggestion(
                actorId, ResourceDtos.Provider.WIKIMEDIA, "fractions", "fr",
                ExternalResource.Cycle.PRIMARY,
                new ResourceDtos.ProviderSearchResult("Fractions", url, "Aperçu", "Wikimedia"));

        assertEquals(existing.getId(), result.id());
        verify(resources, never()).save(any(ExternalResource.class));
        verify(reviews, never()).save(any(ResourceReview.class));
    }

    @Test
    void reimportingTheSameResultReusesTheExistingDraft() {
        String url = "https://fr.wikipedia.org/wiki/Fractions";
        ExternalResource existing = new ExternalResource();
        existing.setId(UUID.randomUUID());
        existing.setUrl(url);
        existing.setReviewStatus(ExternalResource.ReviewStatus.SUBMITTED);
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult("Fractions", url, "Aperçu", "Wikimedia"));
            }

            @Override
            public ProviderRights rightsFor(ProviderSearchResult result) {
                return WikimediaAdapter.wikimediaRights(result);
            }

            @Override
            public String externalIdFor(ProviderSearchResult result) {
                return result.title();
            }
        };
        ResourceService providerService = new ResourceService(resources, reviews, List.of(provider), learningContent);
        when(resources.findByUrl(url)).thenReturn(Optional.of(existing));

        ResourceDtos.AdminResourceView result = providerService.importProviderResult(
                UUID.randomUUID(),
                new ResourceDtos.ImportProviderResourceRequest(
                        ResourceDtos.Provider.WIKIMEDIA,
                        "fractions", url, "fr", ExternalResource.Cycle.BASIC));

        assertEquals(existing.getId(), result.id());
        verify(resources, never()).save(any(ExternalResource.class));
    }

    @Test
    void providerSearchPassesTheSelectedLanguageToAdapters() {
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                assertEquals("fr", language);
                return List.of(new ProviderSearchResult(
                        "Fractions", "https://fr.wikipedia.org/wiki/Fractions", "Aperçu", "Wikimedia"));
            }
        };
        ResourceService providerService = new ResourceService(resources, reviews, List.of(provider), learningContent);

        List<ResourceDtos.ProviderSearchResult> result =
                providerService.searchProviders("fractions", 5, "fr");

        assertEquals(1, result.size());
        assertEquals("https://fr.wikipedia.org/wiki/Fractions", result.get(0).url());
    }

    @Test
    void importsAnOpenStaxResultWithTheAdaptersRightsAndLanguage() {
        UUID actorId = UUID.randomUUID();
        String url = "https://openstax.org/details/books/college-algebra";
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("OPENSTAX", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult("College Algebra", url, "A textbook", "OPENSTAX"));
            }

            @Override
            public ProviderRights rightsFor(ProviderSearchResult result) {
                return new ProviderRights(
                        ExternalResource.Type.DOCUMENT, "CC-BY-4.0",
                        "Creative Commons Attribution 4.0",
                        "https://creativecommons.org/licenses/by/4.0/",
                        "OpenStax, College Algebra (CC BY 4.0)", url);
            }

            @Override
            public String externalIdFor(ProviderSearchResult result) {
                return "college-algebra";
            }

            @Override
            public String resourceLanguage(String selectedLanguage) {
                return "en";
            }
        };
        ResourceService providerService = new ResourceService(resources, reviews, List.of(provider), learningContent);
        when(resources.findByUrl(url)).thenReturn(Optional.empty());
        when(resources.save(any(ExternalResource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceDtos.AdminResourceView result = providerService.importProviderResult(actorId,
                new ResourceDtos.ImportProviderResourceRequest(
                        ResourceDtos.Provider.OPENSTAX,
                        "algebra", url, "fr", ExternalResource.Cycle.SECONDARY));

        assertEquals("OPENSTAX", result.provider());
        assertEquals("en", result.language());
        assertEquals(ExternalResource.Type.DOCUMENT, result.resourceType());
        assertEquals(ExternalResource.ReviewStatus.DRAFT, result.reviewStatus());
        assertEquals("CC-BY-4.0", result.licenceCode());
        assertTrue(result.attributionText().contains("OpenStax"));
        verify(resources, never()).save(argThat(resource -> !"college-algebra".equals(resource.getExternalId())));
    }

    @Test
    void refusesToImportAResultAttributedToAnotherProvider() {
        String url = "https://openstax.org/details/books/college-algebra";
        ResourceProviderAdapter provider = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("OPENSTAX", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult("College Algebra", url, "A textbook", "WIKIMEDIA"));
            }
        };
        ResourceService providerService = new ResourceService(resources, reviews, List.of(provider), learningContent);

        assertThrows(ResourceException.class, () -> providerService.importProviderResult(
                UUID.randomUUID(),
                new ResourceDtos.ImportProviderResourceRequest(
                        ResourceDtos.Provider.OPENSTAX,
                        "algebra", url, "fr", ExternalResource.Cycle.SECONDARY)));

        verify(resources, never()).save(any(ExternalResource.class));
    }

    @Test
    void providerSearchSkipsAnAdapterThatRejectsTheLanguageAndKeepsOtherResults() {
        ResourceProviderAdapter wikimedia = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                throw new IllegalArgumentException("Wikimedia search language must be fr or ar");
            }
        };
        ResourceProviderAdapter openstax = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("OPENSTAX", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult(
                        "Prealgebra", "https://openstax.org/details/books/prealgebra", "Textbook", "OPENSTAX"));
            }
        };
        ResourceService providerService =
                new ResourceService(resources, reviews, List.of(wikimedia, openstax), learningContent);

        List<ResourceDtos.ProviderSearchResult> result =
                providerService.searchProviders("prealgebra", 5, "en");

        assertEquals(1, result.size());
        assertEquals("OPENSTAX", result.get(0).source());
    }

    @Test
    void providerSearchByProviderOnlyCallsTheMatchingAdapter() {
        ResourceProviderAdapter wikimedia = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("WIKIMEDIA", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult(
                        "Fraction", "https://fr.wikipedia.org/wiki/Fraction", "Aperçu", "Wikimedia"));
            }
        };
        ResourceProviderAdapter openstax = new ResourceProviderAdapter() {
            @Override
            public ResourceProviderDescriptor descriptor() {
                return new ResourceProviderDescriptor("OPENSTAX", "ACTIVE", true, "");
            }

            @Override
            public List<ProviderSearchResult> search(String query, int limit, String language) {
                return List.of(new ProviderSearchResult(
                        "Prealgebra", "https://openstax.org/details/books/prealgebra", "Textbook", "OPENSTAX"));
            }
        };
        ResourceService providerService =
                new ResourceService(resources, reviews, List.of(wikimedia, openstax), learningContent);

        List<ResourceDtos.ProviderSearchResult> result =
                providerService.searchProvider(ResourceDtos.Provider.OPENSTAX, "prealgebra", 5, "en");

        assertEquals(1, result.size());
        assertEquals("https://openstax.org/details/books/prealgebra", result.get(0).url());
    }
}
