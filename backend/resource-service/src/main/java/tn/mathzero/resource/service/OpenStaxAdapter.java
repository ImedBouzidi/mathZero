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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tn.mathzero.resource.domain.ExternalResource;

/**
 * Reads OpenStax's documented CMS catalogue. The public catalogue is enough for
 * curation; OAuth client credentials or a bearer token can be supplied when a
 * deployment needs authenticated access. All results are English-language
 * books and remain curation candidates until an administrator approves them.
 */
@Component
public class OpenStaxAdapter implements ResourceProviderAdapter {
    private static final Logger log = LoggerFactory.getLogger(OpenStaxAdapter.class);
    private static final String SOURCE = "OPENSTAX";

    private final boolean enabled;
    private final String baseUrl;
    private final String siteUrl;
    private final String userAgent;
    private final String bearerToken;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Token cachedToken;

    public OpenStaxAdapter(
            @Value("${app.providers.openstax.enabled:false}") boolean enabled,
            @Value("${app.providers.openstax.base-url:https://openstax.org/apps/cms/api/v2}") String baseUrl,
            @Value("${app.providers.openstax.site-url:https://openstax.org}") String siteUrl,
            @Value("${app.providers.openstax.user-agent:MathematicsZeroToHero/0.1}") String userAgent,
            @Value("${app.providers.openstax.bearer-token:}") String bearerToken,
            @Value("${app.providers.openstax.token-url:}") String tokenUrl,
            @Value("${app.providers.openstax.client-id:}") String clientId,
            @Value("${app.providers.openstax.client-secret:}") String clientSecret) {
        this.enabled = enabled;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.siteUrl = trimTrailingSlash(siteUrl);
        this.userAgent = userAgent;
        this.bearerToken = bearerToken;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public ResourceProviderDescriptor descriptor() {
        if (!enabled) {
            return new ResourceProviderDescriptor(
                    SOURCE, "SCAFFOLDED", false,
                    "Search is disabled; enable OPENSTAX_ENABLED to request curation candidates.");
        }
        String auth = isBlank(tokenUrl) || isBlank(clientId) || isBlank(clientSecret)
                ? "Public catalogue; configure OAuth to use an authenticated catalogue."
                : "OAuth client credentials are configured.";
        return new ResourceProviderDescriptor(
                SOURCE, "ACTIVE", true,
                "OpenStax books are English-language textbooks. " + auth
                        + " Verify the book's exact license before approval; nothing is published automatically.");
    }

    @Override
    public List<ProviderSearchResult> search(String query, int limit, String language) {
        if (!enabled || isBlank(query)) {
            return List.of();
        }
        int cappedLimit = Math.max(1, Math.min(limit, 20));
        String uri = baseUrl + "/pages?type=books.Book&search=" + encode(query.trim())
                + "&limit=" + cappedLimit + "&fields=title,description";
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .GET();
            String token = token();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("OpenStax catalogue returned status {}", response.statusCode());
                return List.of();
            }
            JsonNode items = objectMapper.readTree(response.body()).path("items");
            if (!items.isArray()) {
                return List.of();
            }
            List<ProviderSearchResult> results = new ArrayList<>();
            for (JsonNode item : items) {
                String title = item.path("title").asText("").trim();
                if (title.isEmpty()) {
                    continue;
                }
                String url = item.path("meta").path("html_url").asText("").trim();
                if (url.isEmpty() || !url.startsWith("https://")) {
                    String slug = item.path("meta").path("slug").asText("").trim();
                    if (slug.isEmpty()) {
                        continue;
                    }
                    url = siteUrl + "/details/books/" + slug;
                }
                String description = stripHtml(item.path("description").asText(""));
                String snippet = trimToLength(
                        "OpenStax textbook (English). " + description, 300);
                results.add(new ProviderSearchResult(title, url, snippet, SOURCE));
                if (results.size() >= cappedLimit) {
                    break;
                }
            }
            return List.copyOf(results);
        } catch (Exception exception) {
            log.error("OpenStaxAdapter search failed for query '{}': {}", query, exception.getMessage(), exception);
            return List.of();
        }
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
    public boolean acceptsUrl(String url, String language) {
        return matchesSite(url, "/details/books/");
    }

    @Override
    public String resourceLanguage(String selectedLanguage) {
        // OpenStax's catalogue is English-language; do not mislabel it as fr/ar.
        return "en";
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

    private String token() {
        if (!isBlank(bearerToken)) {
            return bearerToken.trim();
        }
        if (isBlank(tokenUrl) || isBlank(clientId) || isBlank(clientSecret)) {
            return null;
        }
        Token current = cachedToken;
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.value();
        }
        synchronized (this) {
            current = cachedToken;
            if (current != null && Instant.now().isBefore(current.expiresAt())) {
                return current.value();
            }
            try {
                String form = "grant_type=client_credentials&client_id=" + encode(clientId.trim())
                        + "&client_secret=" + encode(clientSecret.trim());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl.trim()))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .header("User-Agent", userAgent)
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.warn("OpenStax OAuth token endpoint returned status {}", response.statusCode());
                    return null;
                }
                JsonNode root = objectMapper.readTree(response.body());
                String value = root.path("access_token").asText("").trim();
                if (value.isEmpty()) {
                    return null;
                }
                long expiresIn = root.path("expires_in").asLong(3600);
                Token fresh = new Token(
                        value, Instant.now().plusSeconds(Math.max(60, expiresIn - 30)));
                cachedToken = fresh;
                return value;
            } catch (Exception exception) {
                log.warn("OpenStax OAuth token request failed: {}", exception.getMessage());
                return null;
            }
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripHtml(String value) {
        String unescaped = HtmlUtils.htmlUnescape(value == null ? "" : value);
        return unescaped.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private static String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Token(String value, Instant expiresAt) {}
}
