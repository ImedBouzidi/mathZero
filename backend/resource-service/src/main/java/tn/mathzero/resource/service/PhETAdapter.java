package tn.mathzero.resource.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tn.mathzero.resource.domain.ExternalResource;

/**
 * Reads PhET's public simulation metadata. The catalogue is localized in French
 * and Arabic and is cached briefly because the full document is large. The
 * recorded license comes from each simulation's own metadata; PhET material may
 * carry non-commercial terms, so nothing is approved automatically.
 */
@Component
public class PhETAdapter implements ResourceProviderAdapter {
    private static final Logger log = LoggerFactory.getLogger(PhETAdapter.class);
    private static final String SOURCE = "PHET";

    private final boolean enabled;
    private final String baseUrl;
    private final String siteUrl;
    private final String userAgent;
    private final long cacheSeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> licenseByUrl = new ConcurrentHashMap<>();
    private volatile Cached cached;

    public PhETAdapter(
            @Value("${app.providers.phet.enabled:false}") boolean enabled,
            @Value("${app.providers.phet.base-url:https://phet.colorado.edu/services/metadata/1.3/simulations}") String baseUrl,
            @Value("${app.providers.phet.site-url:https://phet.colorado.edu}") String siteUrl,
            @Value("${app.providers.phet.user-agent:MathematicsZeroToHero/0.1}") String userAgent,
            @Value("${app.providers.phet.cache-seconds:3600}") long cacheSeconds) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.siteUrl = siteUrl.replaceAll("/+$", "");
        this.userAgent = userAgent;
        this.cacheSeconds = Math.max(60, cacheSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public ResourceProviderDescriptor descriptor() {
        if (!enabled) {
            return new ResourceProviderDescriptor(
                    SOURCE, "SCAFFOLDED", false,
                    "Search is disabled; enable PHET_ENABLED to request curation candidates.");
        }
        return new ResourceProviderDescriptor(
                SOURCE, "ACTIVE", true,
                "PhET simulations are interactive and localized. Check each simulation's license "
                        + "because non-commercial terms may apply; nothing is published automatically.");
    }

    @Override
    public List<ProviderSearchResult> search(String query, int limit, String language) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }
        String locale = "ar".equalsIgnoreCase(language) ? "ar" : "fr";
        JsonNode catalogue = metadata(locale);
        if (catalogue == null) {
            return List.of();
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        int cappedLimit = Math.max(1, Math.min(limit, 20));
        List<Scored> matches = new ArrayList<>();

        for (JsonNode project : catalogue.path("projects")) {
            for (JsonNode simulation : project.path("simulations")) {
                JsonNode localized = simulation.path("localizedSimulations").path(locale);
                if (!localized.isObject()) {
                    continue;
                }
                String title = localized.path("title").asText("").trim();
                String description = stripHtml(localized.path("description").asText(""));
                if (title.isEmpty()
                        || (!title.toLowerCase(Locale.ROOT).contains(needle)
                                && !description.toLowerCase(Locale.ROOT).contains(needle))) {
                    continue;
                }
                String path = localized.path("simPageUrl").asText("").trim();
                if (path.isEmpty()) {
                    continue;
                }
                String url = siteUrl + (path.startsWith("/") ? path : "/" + path);
                licenseByUrl.put(url, licenseCode(simulation.path("licenseUrls")));
                int score = title.toLowerCase(Locale.ROOT).contains(needle) ? 2 : 1;
                matches.add(new Scored(score, new ProviderSearchResult(
                        title, url, trimToLength(description, 300), SOURCE)));
            }
        }

        return matches.stream()
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(item -> item.result().title()))
                .map(Scored::result)
                .limit(cappedLimit)
                .toList();
    }

    @Override
    public ProviderRights rightsFor(ProviderSearchResult result) {
        String code = licenseByUrl.getOrDefault(result.url(), "PHET_TERMS");
        String name = switch (code) {
            case "CC-BY-4.0" -> "Creative Commons Attribution 4.0";
            case "CC-BY-NC-4.0" -> "Creative Commons Attribution-NonCommercial 4.0";
            case "CC-BY-NC-SA-4.0" -> "Creative Commons Attribution-NonCommercial-ShareAlike 4.0";
            default -> "PhET Terms of Use";
        };
        String url = switch (code) {
            case "CC-BY-4.0" -> "https://creativecommons.org/licenses/by/4.0/";
            case "CC-BY-NC-4.0" -> "https://creativecommons.org/licenses/by-nc/4.0/";
            case "CC-BY-NC-SA-4.0" -> "https://creativecommons.org/licenses/by-nc-sa/4.0/";
            default -> "https://phet.colorado.edu/en/license";
        };
        return new ProviderRights(
                ExternalResource.Type.INTERACTIVE,
                code,
                name,
                url,
                "PhET Interactive Simulations, University of Colorado Boulder, "
                        + result.title() + " (" + name + ")",
                result.url());
    }

    @Override
    public boolean acceptsUrl(String url, String language) {
        return matchesSite(url, "/simulations/");
    }

    @Override
    public String externalIdFor(ProviderSearchResult result) {
        String path = URI.create(result.url()).getPath();
        if (path == null) {
            return result.url();
        }
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : result.url();
    }

    @Override
    public String resourceLanguage(String selectedLanguage) {
        return "ar".equalsIgnoreCase(selectedLanguage) ? "ar" : "fr";
    }

    private JsonNode metadata(String locale) {
        Cached current = cached;
        if (current != null && locale.equals(current.locale()) && Instant.now().isBefore(current.expiresAt())) {
            return current.data();
        }
        synchronized (this) {
            current = cached;
            if (current != null && locale.equals(current.locale()) && Instant.now().isBefore(current.expiresAt())) {
                return current.data();
            }
            try {
                String uri = baseUrl + "?format=json&locale=" + URLEncoder.encode(locale, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .header("User-Agent", userAgent)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.warn("PhET metadata returned status {}", response.statusCode());
                    return null;
                }
                JsonNode root = objectMapper.readTree(response.body());
                if (!root.path("projects").isArray()) {
                    return null;
                }
                cached = new Cached(locale, root, Instant.now().plusSeconds(cacheSeconds));
                return root;
            } catch (Exception exception) {
                log.error("PhETAdapter metadata load failed: {}", exception.getMessage(), exception);
                return null;
            }
        }
    }

    private boolean matchesSite(String value, String pathContains) {
        try {
            URI configured = URI.create(siteUrl);
            URI uri = URI.create(value == null ? "" : value.trim());
            return configured.getScheme().equalsIgnoreCase(uri.getScheme())
                    && configured.getHost().equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().contains(pathContains);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String licenseCode(JsonNode licenseUrls) {
        if (licenseUrls.isArray()) {
            for (JsonNode entry : licenseUrls) {
                String value = entry.asText("").toLowerCase(Locale.ROOT);
                if (value.contains("by-nc-sa")) {
                    return "CC-BY-NC-SA-4.0";
                }
                if (value.contains("by-nc")) {
                    return "CC-BY-NC-4.0";
                }
                if (value.contains("/by/")) {
                    return "CC-BY-4.0";
                }
            }
        }
        return "PHET_TERMS";
    }

    private static String stripHtml(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value)
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String trimToLength(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record Cached(String locale, JsonNode data, Instant expiresAt) {}

    private record Scored(int score, ProviderSearchResult result) {}
}
