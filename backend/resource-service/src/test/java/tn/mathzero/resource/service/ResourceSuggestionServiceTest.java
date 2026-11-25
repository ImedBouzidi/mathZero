package tn.mathzero.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.mathzero.resource.api.ResourceDtos;
import tn.mathzero.resource.config.ResourceSuggestionProperties;
import tn.mathzero.resource.domain.ExternalResource;

@ExtendWith(MockitoExtension.class)
class ResourceSuggestionServiceTest {
    @Mock private ResourceService resourceService;

    private ResourceSuggestionProperties properties;
    private ResourceSuggestionService service;

    @BeforeEach
    void setUp() {
        properties = new ResourceSuggestionProperties();
        properties.setCacheTtl(Duration.ofMinutes(15));
        properties.setLimitPerQuery(5);
        properties.setMaxPerCycle(10);
        properties.setProviders(Map.of(
                "wikimedia", new ResourceSuggestionProperties.ProviderQueries(Map.of(
                        ExternalResource.Cycle.PRIMARY, Map.of("fr", List.of("fractions")))),
                "openstax", new ResourceSuggestionProperties.ProviderQueries(Map.of(
                        ExternalResource.Cycle.SECONDARY, Map.of("en", List.of("functions")))),
                "geogebra", new ResourceSuggestionProperties.ProviderQueries(Map.of(
                        ExternalResource.Cycle.BASIC, Map.of("ar", List.of("هندسة")))),
                "phet", new ResourceSuggestionProperties.ProviderQueries(Map.of())));
        service = new ResourceSuggestionService(resourceService, properties);
    }

    @Test
    void groupsCandidatesFromEveryConfiguredProviderByCycleAndDeduplicatesByUrl() {
        String wikipediaUrl = "https://fr.wikipedia.org/wiki/Fraction_(math%C3%A9matiques)";
        String openStaxUrl = "https://openstax.org/details/books/algebra-and-trigonometry";
        String geoGebraUrl = "https://www.geogebra.org/m/abc123";
        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult("Fraction", wikipediaUrl, "Aperçu", "Wikimedia"),
                new ResourceDtos.ProviderSearchResult("Fraction", wikipediaUrl, "Aperçu", "Wikimedia")));
        when(resourceService.searchProvider(ResourceDtos.Provider.OPENSTAX, "functions", 5, "en")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult(
                        "Algebra and Trigonometry", openStaxUrl, "English textbook", "OPENSTAX")));
        when(resourceService.searchProvider(ResourceDtos.Provider.GEOGEBRA, "هندسة", 5, "ar")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult("هندسة", geoGebraUrl, " applet", "GEOGEBRA")));

        List<ResourceDtos.Suggestion> suggestions = service.suggestions();

        assertEquals(3, suggestions.size());
        ResourceDtos.Suggestion wikipedia = suggestions.stream()
                .filter(item -> item.provider() == ResourceDtos.Provider.WIKIMEDIA)
                .findFirst()
                .orElseThrow();
        assertEquals(ExternalResource.Cycle.PRIMARY, wikipedia.cycle());
        assertEquals("fr", wikipedia.language());
        assertEquals("fractions", wikipedia.query());
        assertEquals(wikipediaUrl, wikipedia.url());

        ResourceDtos.Suggestion openStax = suggestions.stream()
                .filter(item -> item.provider() == ResourceDtos.Provider.OPENSTAX)
                .findFirst()
                .orElseThrow();
        assertEquals(ExternalResource.Cycle.SECONDARY, openStax.cycle());
        assertEquals("en", openStax.language());
        assertEquals("functions", openStax.query());

        ResourceDtos.Suggestion geogebra = suggestions.stream()
                .filter(item -> item.provider() == ResourceDtos.Provider.GEOGEBRA)
                .findFirst()
                .orElseThrow();
        assertEquals(ExternalResource.Cycle.BASIC, geogebra.cycle());
        assertEquals("ar", geogebra.language());
        assertEquals("هندسة", geogebra.query());
        verify(resourceService, times(1)).searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr");
        verify(resourceService, times(1)).searchProvider(ResourceDtos.Provider.OPENSTAX, "functions", 5, "en");
        verify(resourceService, times(1)).searchProvider(ResourceDtos.Provider.GEOGEBRA, "هندسة", 5, "ar");
    }

    @Test
    void appliesThePerCycleCapPerProviderInsteadOfStarvingOtherProviders() {
        ResourceSuggestionProperties capped = new ResourceSuggestionProperties();
        capped.setCacheTtl(Duration.ofMinutes(15));
        capped.setLimitPerQuery(5);
        capped.setMaxPerCycle(1);
        Map<String, ResourceSuggestionProperties.ProviderQueries> providers = new LinkedHashMap<>();
        providers.put("wikimedia", new ResourceSuggestionProperties.ProviderQueries(Map.of(
                ExternalResource.Cycle.PRIMARY, Map.of("fr", List.of("fractions")))));
        providers.put("openstax", new ResourceSuggestionProperties.ProviderQueries(Map.of(
                ExternalResource.Cycle.PRIMARY, Map.of("en", List.of("prealgebra")))));
        capped.setProviders(providers);
        ResourceSuggestionService cappedService = new ResourceSuggestionService(resourceService, capped);

        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult(
                        "Fraction", "https://fr.wikipedia.org/wiki/Fraction", "Aperçu", "Wikimedia")));
        when(resourceService.searchProvider(ResourceDtos.Provider.OPENSTAX, "prealgebra", 5, "en")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult(
                        "Prealgebra", "https://openstax.org/details/books/prealgebra", "Textbook", "OPENSTAX")));

        List<ResourceDtos.Suggestion> suggestions = cappedService.suggestions();

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream()
                .anyMatch(item -> item.provider() == ResourceDtos.Provider.WIKIMEDIA));
        assertTrue(suggestions.stream()
                .anyMatch(item -> item.provider() == ResourceDtos.Provider.OPENSTAX));
    }

    @Test
    void keepsBothLanguagesForAProviderWhenOneLanguageFillsTheCap() {
        ResourceSuggestionProperties capped = new ResourceSuggestionProperties();
        capped.setCacheTtl(Duration.ofMinutes(15));
        capped.setLimitPerQuery(5);
        capped.setMaxPerCycle(1);
        capped.setProviders(Map.of(
                "wikimedia", new ResourceSuggestionProperties.ProviderQueries(Map.of(
                        ExternalResource.Cycle.PRIMARY, Map.of(
                                "fr", List.of("fractions"),
                                "ar", List.of("الكسور"))))));
        ResourceSuggestionService cappedService = new ResourceSuggestionService(resourceService, capped);

        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr"))
                .thenReturn(List.of(new ResourceDtos.ProviderSearchResult(
                        "Fraction", "https://fr.wikipedia.org/wiki/Fraction", "Aperçu", "Wikimedia")));
        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "الكسور", 5, "ar"))
                .thenReturn(List.of(new ResourceDtos.ProviderSearchResult(
                        "كسور", "https://ar.wikipedia.org/wiki/كسور", "ملخص", "Wikimedia")));

        List<ResourceDtos.Suggestion> suggestions = cappedService.suggestions();

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(item -> "fr".equals(item.language())));
        assertTrue(suggestions.stream().anyMatch(item -> "ar".equals(item.language())));
    }

    @Test
    void approvesOnlyASuggestionFromTheCurrentBoardAndUsesItsProvider() {
        String url = "https://openstax.org/details/books/algebra-and-trigonometry";
        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr")).thenReturn(List.of());
        when(resourceService.searchProvider(ResourceDtos.Provider.OPENSTAX, "functions", 5, "en")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult(
                        "Algebra and Trigonometry", url, "English textbook", "OPENSTAX")));
        when(resourceService.searchProvider(ResourceDtos.Provider.GEOGEBRA, "هندسة", 5, "ar")).thenReturn(List.of());
        UUID actorId = UUID.randomUUID();
        ResourceDtos.ApproveSuggestionRequest request = new ResourceDtos.ApproveSuggestionRequest(
                ResourceDtos.Provider.OPENSTAX,
                ExternalResource.Cycle.SECONDARY,
                "en",
                "functions",
                url);

        service.approve(actorId, request);

        verify(resourceService).createApprovedFromSuggestion(
                actorId,
                ResourceDtos.Provider.OPENSTAX,
                "functions",
                "en",
                ExternalResource.Cycle.SECONDARY,
                new ResourceDtos.ProviderSearchResult(
                        "Algebra and Trigonometry", url, "English textbook", "OPENSTAX"));
    }

    @Test
    void rejectsAnApprovalWithAMismatchedProvider() {
        String url = "https://fr.wikipedia.org/wiki/Fraction_(math%C3%A9matiques)";
        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr")).thenReturn(List.of(
                new ResourceDtos.ProviderSearchResult("Fraction", url, "Aperçu", "Wikimedia")));
        when(resourceService.searchProvider(ResourceDtos.Provider.OPENSTAX, "functions", 5, "en")).thenReturn(List.of());
        when(resourceService.searchProvider(ResourceDtos.Provider.GEOGEBRA, "هندسة", 5, "ar")).thenReturn(List.of());

        assertThrows(ResourceException.class,
                () -> service.approve(UUID.randomUUID(), new ResourceDtos.ApproveSuggestionRequest(
                        ResourceDtos.Provider.PHET,
                        ExternalResource.Cycle.PRIMARY,
                        "fr",
                        "fractions",
                        url)));

        verify(resourceService, never()).createApprovedFromSuggestion(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsAnUrlThatIsNotOnTheBoard() {
        when(resourceService.searchProvider(ResourceDtos.Provider.WIKIMEDIA, "fractions", 5, "fr")).thenReturn(List.of());
        when(resourceService.searchProvider(ResourceDtos.Provider.OPENSTAX, "functions", 5, "en")).thenReturn(List.of());
        when(resourceService.searchProvider(ResourceDtos.Provider.GEOGEBRA, "هندسة", 5, "ar")).thenReturn(List.of());

        assertThrows(ResourceException.class,
                () -> service.approve(UUID.randomUUID(), new ResourceDtos.ApproveSuggestionRequest(
                        ResourceDtos.Provider.WIKIMEDIA,
                        ExternalResource.Cycle.PRIMARY,
                        "fr",
                        "fractions",
                        "https://fr.wikipedia.org/wiki/Unknown")));

        verify(resourceService, never()).createApprovedFromSuggestion(
                any(), any(), any(), any(), any(), any());
    }
}
