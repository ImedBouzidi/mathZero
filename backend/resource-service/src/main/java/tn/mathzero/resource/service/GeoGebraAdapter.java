package tn.mathzero.resource.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tn.mathzero.resource.domain.ExternalResource;

/**
 * Searches GeoGebra's documented material search endpoint. Materials are
 * interactive applets; the recorded licence is GeoGebra's non-commercial
 * licence, so an administrator must check the intended use before approval.
 */
@Component
public class GeoGebraAdapter implements ResourceProviderAdapter {
    private static final Logger log = LoggerFactory.getLogger(GeoGebraAdapter.class);
    private static final String SOURCE = "GEOGEBRA";

    private final boolean enabled;
    private final String baseUrl;
    private final String siteUrl;
    private final String userAgent;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeoGebraAdapter(
            @Value("${app.providers.geogebra.enabled:false}") boolean enabled,
            @Value("${app.providers.geogebra.base-url:https://api.geogebra.org/v1.0}") String baseUrl,
            @Value("${app.providers.geogebra.site-url:https://www.geogebra.org}") String siteUrl,
            @Value("${app.providers.geogebra.user-agent:MathematicsZeroToHero/0.1}") String userAgent) {
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.siteUrl = siteUrl.replaceAll("/+$", "");
        this.userAgent = userAgent;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public ResourceProviderDescriptor descriptor() {
        if (!enabled) {
            return new ResourceProviderDescriptor(
                    SOURCE, "SCAFFOLDED", false,
                    "Search is disabled; enable GEOGEBRA_ENABLED to request curation candidates.");
        }
        return new ResourceProviderDescriptor(
                SOURCE, "ACTIVE", true,
                "GeoGebra applets are licensed for non-commercial use by default; "
                        + "verify the material and its rights before approval. Nothing is published automatically.");
    }

    @Override
    public List<ProviderSearchResult> search(String query, int limit, String language) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedLanguage = "ar".equalsIgnoreCase(language) ? "ar" : "fr";
        String uri = baseUrl + "/search/materials?query=" + encode(query.trim())
                + "&lang=" + normalizedLanguage;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("GeoGebra material search returned status {}", response.statusCode());
                return List.of();
            }
            JsonNode hits = objectMapper.readTree(response.body()).path("hits");
            if (!hits.isArray()) {
                return List.of();
            }
            int cappedLimit = Math.max(1, Math.min(limit, 20));
            List<ProviderSearchResult> results = new ArrayList<>();
            for (JsonNode hit : hits) {
                String id = hit.path("id").asText("").trim();
                String title = hit.path("title").asText("").trim();
                if (id.isEmpty() || title.isEmpty()) {
                    continue;
                }
                String creator = hit.path("creator").path("name").asText("").trim();
                String snippet = "GeoGebra " + hit.path("type").asText("material")
                        + (creator.isEmpty() ? "" : " by " + creator)
                        + ". Verify the material and its non-commercial license before approval.";
                results.add(new ProviderSearchResult(title, siteUrl + "/m/" + id, snippet, SOURCE));
                if (results.size() >= cappedLimit) {
                    break;
                }
            }
            return List.copyOf(results);
        } catch (Exception exception) {
            log.error("GeoGebraAdapter search failed for query '{}': {}", query, exception.getMessage(), exception);
            return List.of();
        }
    }

    @Override
    public ProviderRights rightsFor(ProviderSearchResult result) {
        return new ProviderRights(
                ExternalResource.Type.INTERACTIVE,
                "GEOGEBRA_NONCOMMERCIAL",
                "GeoGebra Non-Commercial License",
                "https://www.geogebra.org/license",
                "GeoGebra, " + result.title() + " (GeoGebra Non-Commercial License)",
                result.url());
    }

    @Override
    public boolean acceptsUrl(String url, String language) {
        return matchesSite(url, "/m/");
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

    private boolean matchesSite(String value, String pathPrefix) {
        try {
            URI configured = URI.create(siteUrl);
            URI uri = URI.create(value == null ? "" : value.trim());
            return configured.getScheme().equalsIgnoreCase(uri.getScheme())
                    && configured.getHost().equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith(pathPrefix);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
