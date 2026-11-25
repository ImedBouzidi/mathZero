package tn.mathzero.resource.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

import tn.mathzero.resource.api.ResourceDtos;
import tn.mathzero.resource.config.ResourceSuggestionProperties;

/**
 * Builds a ready-to-review board across every enabled provider: one group per
 * experience cycle, using the configured topic queries. Candidates are cached
 * briefly so opening the admin page does not hit external catalogues on every
 * load. Approval is routed through the owning adapter so rights metadata always
 * comes from the server.
 */
@Service
public class ResourceSuggestionService {
    private static final List<String> ALLOWED_LANGUAGES = List.of("fr", "ar", "en");

    private final ResourceService resourceService;
    private final ResourceSuggestionProperties properties;

    private volatile List<ResourceDtos.Suggestion> cache = List.of();
    private volatile Instant expiresAt = Instant.EPOCH;

    public ResourceSuggestionService(
            ResourceService resourceService, ResourceSuggestionProperties properties) {
        this.resourceService = resourceService;
        this.properties = properties;
    }

    public List<ResourceDtos.Suggestion> suggestions() {
        if (Instant.now().isBefore(expiresAt)) {
            return cache;
        }
        synchronized (this) {
            if (Instant.now().isBefore(expiresAt)) {
                return cache;
            }
            cache = load();
            expiresAt = Instant.now().plus(properties.getCacheTtl());
            return cache;
        }
    }

    public ResourceDtos.AdminResourceView approve(
            UUID actorId, ResourceDtos.ApproveSuggestionRequest request) {
        String language = request.language() == null ? "" : request.language().trim().toLowerCase(Locale.ROOT);
        String query = request.query() == null ? "" : request.query().trim();
        String resultUrl = request.resultUrl() == null ? "" : request.resultUrl().trim();

        ResourceDtos.Suggestion suggestion = suggestions().stream()
                .filter(item -> item.provider() == request.provider())
                .filter(item -> item.cycle() == request.cycle())
                .filter(item -> item.language().equals(language))
                .filter(item -> item.query().equals(query))
                .filter(item -> item.url().equals(resultUrl))
                .findFirst()
                .orElseThrow(() -> ResourceException.invalid(
                        "This suggestion is no longer available; reload the board"));

        return resourceService.createApprovedFromSuggestion(
                actorId,
                suggestion.provider(),
                suggestion.query(),
                suggestion.language(),
                suggestion.cycle(),
                new ResourceDtos.ProviderSearchResult(
                        suggestion.title(), suggestion.url(), suggestion.snippet(), suggestion.source()));
    }

    private List<ResourceDtos.Suggestion> load() {
        if (properties.getProviders() == null || properties.getProviders().isEmpty()) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(properties.getLimitPerQuery(), 20));
        Queue<ResourceDtos.Suggestion> collected = new ConcurrentLinkedQueue<>();

        properties.getProviders().forEach((providerName, providerQueries) -> {
            ResourceDtos.Provider provider = parseProvider(providerName);
            if (provider == null || providerQueries == null || providerQueries.getQueries() == null) {
                return;
            }
            providerQueries.getQueries().forEach((cycle, byLanguage) -> {
                if (cycle == null || byLanguage == null) {
                    return;
                }
                byLanguage.forEach((language, queries) -> {
                    String normalized = language == null ? "" : language.toLowerCase(Locale.ROOT);
                    if (!ALLOWED_LANGUAGES.contains(normalized) || queries == null) {
                        return;
                    }
                    queries.parallelStream().forEach(query -> {
                        if (query == null || query.isBlank()) {
                            return;
                        }
                        String trimmedQuery = query.trim();
                        for (ResourceDtos.ProviderSearchResult item
                                : resourceService.searchProvider(provider, trimmedQuery, limit, normalized)) {
                            collected.add(new ResourceDtos.Suggestion(
                                    provider, cycle, normalized, trimmedQuery, item.title(),
                                    item.url(), item.snippet(), item.source()));
                        }
                    });
                });
            });
        });

        Map<String, ResourceDtos.Suggestion> byUrl = new LinkedHashMap<>();
        for (ResourceDtos.Suggestion suggestion : collected) {
            byUrl.putIfAbsent(suggestion.url(), suggestion);
        }
        // Cap each provider and language independently. A single global cap per
        // cycle would let the first provider in configuration order fill every
        // slot and starve the others; sharing the cap across languages would
        // also drop the Arabic board once the French queries fill it.
        int maxPerCycle = Math.max(1, properties.getMaxPerCycle());
        Map<String, Integer> perProviderCycleLanguage = new LinkedHashMap<>();
        List<ResourceDtos.Suggestion> result = new ArrayList<>();
        for (ResourceDtos.Suggestion suggestion : byUrl.values()) {
            String key = suggestion.provider().name()
                    + ":" + suggestion.cycle().name()
                    + ":" + suggestion.language();
            int count = perProviderCycleLanguage.getOrDefault(key, 0);
            if (count < maxPerCycle) {
                result.add(suggestion);
                perProviderCycleLanguage.put(key, count + 1);
            }
        }
        return List.copyOf(result);
    }

    private static ResourceDtos.Provider parseProvider(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ResourceDtos.Provider.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
