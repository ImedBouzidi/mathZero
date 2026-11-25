package tn.mathzero.resource.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import tn.mathzero.resource.domain.ExternalResource;

@Component
public class WikimediaAdapter implements ResourceProviderAdapter {
    private static final Logger log = LoggerFactory.getLogger(WikimediaAdapter.class);

    private final boolean enabled;
    private final String baseUrl;
    private final String userAgent;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WikimediaAdapter(
            @Value("${app.providers.wikimedia.enabled:false}") boolean enabled,
            @Value("${app.providers.wikimedia.base-url:https://en.wikipedia.org/w/api.php}") String baseUrl,
            @Value("${app.providers.wikimedia.user-agent:MathsPlay/0.1}") String userAgent) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        // The MediaWiki search response carries many fields we do not map
        // (ns, pageid, size, timestamp, ...); ignore them instead of failing.
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public ResourceProviderDescriptor descriptor() {
        if (!enabled) {
            return new ResourceProviderDescriptor(
                    "WIKIMEDIA", "SCAFFOLDED", false,
                    "Search is disabled; enable WIKIMEDIA_ENABLED to request curation candidates.");
        }
        return new ResourceProviderDescriptor(
                "WIKIMEDIA", "ACTIVE", true,
                "Wikimedia search returns candidates for manual review; nothing is imported or published automatically.");
    }

    @Override
    public List<ProviderSearchResult> search(String query, int limit) {
        return search(query, limit, "fr");
    }

    @Override
    public List<ProviderSearchResult> search(String query, int limit, String language) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }
        if (!"fr".equals(language) && !"ar".equals(language)) {
            throw new IllegalArgumentException("Wikimedia search language must be fr or ar");
        }

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String localizedBaseUrl = baseUrl.replace("en.wikipedia.org", language + ".wikipedia.org");
        String uri = localizedBaseUrl + "?action=query&list=search&srsearch=" + encoded
                + "&srlimit=" + Math.min(limit, 20)
                + "&format=json&formatversion=2";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Wikimedia API returned status {}", response.statusCode());
                return List.of();
            }

            WikimediaSearchResponse parsed = objectMapper.readValue(response.body(), WikimediaSearchResponse.class);
            List<WikimediaSearchResponse.SearchItem> searchItems = parsed.query().search();
            return searchItems.stream()
                    .map(item -> new ProviderSearchResult(
                            item.title(),
                            "https://" + language + ".wikipedia.org/wiki/"
                                    + URLEncoder.encode(item.title().replace(' ', '_'), StandardCharsets.UTF_8),
                            HtmlUtils.htmlUnescape(item.snippet().replaceAll("<[^>]*>", "")),
                            "Wikimedia"))
                    .toList();
        } catch (Exception exception) {
            log.error("WikimediaAdapter search failed for query '{}': {}", query, exception.getMessage(), exception);
            return List.of();
        }
    }

    @Override
    public ProviderRights rightsFor(ProviderSearchResult result) {
        return wikimediaRights(result);
    }

    @Override
    public boolean acceptsUrl(String url, String language) {
        try {
            URI uri = URI.create(url == null ? "" : url.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && (language + ".wikipedia.org").equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/wiki/");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public String externalIdFor(ProviderSearchResult result) {
        return result.title();
    }

    /** Shared by the suggestion approval path, which has no adapter lookup. */
    public static ProviderRights wikimediaRights(ProviderSearchResult result) {
        return new ProviderRights(
                ExternalResource.Type.ARTICLE,
                "CC-BY-SA-4.0",
                "Creative Commons Attribution-ShareAlike 4.0",
                "https://creativecommons.org/licenses/by-sa/4.0/",
                "Wikipedia contributors, " + result.title() + " (CC BY-SA 4.0)",
                result.url());
    }

    record WikimediaSearchResponse(Query query) {
        record Query(List<SearchItem> search) {}
        record SearchItem(String title, String snippet) {}
    }
}
