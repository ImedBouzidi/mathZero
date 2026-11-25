package tn.mathzero.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tn.mathzero.resource.domain.ExternalResource;

/** Adapter-level tests against a local HTTP stub; no external calls are made. */
class ExternalProviderAdaptersTest {
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void openStaxSearchesTheCatalogueAndUsesOAuthWhenConfigured() {
        OAUTH_AUTH_HEADER.set(null);
        server.createContext("/cms/pages", exchange -> respond(exchange, 200, """
                {"items":[{"title":"College Algebra",
                  "meta":{"html_url":"%s/details/books/college-algebra","slug":"college-algebra"},
                  "description":"<p>Algebra <b>textbook</b>.</p>"}]}
                """.formatted(baseUrl())));
        server.createContext("/token", exchange ->
                respond(exchange, 200, "{\"access_token\":\"test-token\",\"expires_in\":3600}"));

        OpenStaxAdapter adapter = new OpenStaxAdapter(
                true, baseUrl() + "/cms", baseUrl(), "MathsPlay test", "",
                baseUrl() + "/token", "client", "secret");
        List<ResourceProviderAdapter.ProviderSearchResult> results = adapter.search("algebra", 5, "fr");

        assertEquals(1, results.size());
        ResourceProviderAdapter.ProviderSearchResult result = results.get(0);
        assertEquals("College Algebra", result.title());
        assertEquals(baseUrl() + "/details/books/college-algebra", result.url());
        assertEquals("OPENSTAX", result.source());
        assertTrue(result.snippet().contains("Algebra textbook"));
        assertTrue(result.snippet().contains("English"));
        assertEquals("Bearer test-token", capturedAuthHeader());
        assertTrue(adapter.acceptsUrl(result.url(), "fr"));
        assertFalse(adapter.acceptsUrl("https://example.org/details/books/college-algebra", "fr"));
        assertEquals("en", adapter.resourceLanguage("fr"));
        assertEquals("college-algebra", baseUrlSlug(result.url()));

        ResourceProviderAdapter.ProviderRights rights = adapter.rightsFor(result);
        assertEquals(ExternalResource.Type.DOCUMENT, rights.type());
        assertEquals("CC-BY-4.0", rights.licenceCode());
        assertEquals("https://creativecommons.org/licenses/by/4.0/", rights.licenceUrl());
    }

    @Test
    void geoGebraSearchesMaterialsAndRecordsNonCommercialRights() {
        AtomicReference<String> query = new AtomicReference<>();
        server.createContext("/search/materials", exchange -> {
            query.set(exchange.getRequestURI().getQuery());
            respond(exchange, 200, """
                    {"hits":[{"id":"abc123","title":"Fraction Exploration","type":"ws",
                      "creator":{"name":"A. Teacher"},"visibility":"O"}]}
                    """);
        });

        GeoGebraAdapter adapter = new GeoGebraAdapter(
                true, baseUrl(), baseUrl(), "MathsPlay test");
        List<ResourceProviderAdapter.ProviderSearchResult> results = adapter.search("fraction", 5, "fr");

        assertEquals(1, results.size());
        ResourceProviderAdapter.ProviderSearchResult result = results.get(0);
        assertEquals("Fraction Exploration", result.title());
        assertEquals(baseUrl() + "/m/abc123", result.url());
        assertEquals("GEOGEBRA", result.source());
        assertTrue(result.snippet().contains("by A. Teacher"));
        assertTrue(query.get().contains("lang=fr"));

        ResourceProviderAdapter.ProviderRights rights = adapter.rightsFor(result);
        assertEquals(ExternalResource.Type.INTERACTIVE, rights.type());
        assertEquals("GEOGEBRA_NONCOMMERCIAL", rights.licenceCode());
        assertEquals("https://www.geogebra.org/license", rights.licenceUrl());
        assertEquals("abc123", adapter.externalIdFor(result));
        assertTrue(adapter.acceptsUrl(result.url(), "fr"));
    }

    @Test
    void phetUsesLocalizedMetadataAndTheSimulationsOwnLicense() {
        server.createContext("/metadata", exchange -> respond(exchange, 200, """
                {"projects":[{"simulations":[
                  {"id":42,"name":"fraction-matcher","licenseUrls":
                    ["http://creativecommons.org/licenses/by-nc/4.0/"],
                   "localizedSimulations":{
                     "fr":{"title":"Fractions équivalentes",
                           "description":"<p>Associer des fractions</p>",
                           "simPageUrl":"/fr/simulations/fraction-matcher"},
                     "ar":{"title":"كسور متكافئة",
                           "description":"<p>ربط الكسور</p>",
                           "simPageUrl":"/ar/simulations/fraction-matcher"}}}]}]}
                """));

        PhETAdapter adapter = new PhETAdapter(
                true, baseUrl() + "/metadata", baseUrl(), "MathsPlay test", 60);
        List<ResourceProviderAdapter.ProviderSearchResult> results = adapter.search("fraction", 5, "fr");

        assertEquals(1, results.size());
        ResourceProviderAdapter.ProviderSearchResult result = results.get(0);
        assertEquals("Fractions équivalentes", result.title());
        assertEquals(baseUrl() + "/fr/simulations/fraction-matcher", result.url());
        assertEquals("PHET", result.source());
        assertEquals("Associer des fractions", result.snippet());

        ResourceProviderAdapter.ProviderRights rights = adapter.rightsFor(result);
        assertEquals(ExternalResource.Type.INTERACTIVE, rights.type());
        assertEquals("CC-BY-NC-4.0", rights.licenceCode());
        assertEquals("https://creativecommons.org/licenses/by-nc/4.0/", rights.licenceUrl());
        assertTrue(adapter.acceptsUrl(result.url(), "fr"));
        assertEquals("fraction-matcher", adapter.externalIdFor(result));

        List<ResourceProviderAdapter.ProviderSearchResult> arabic =
                adapter.search("كسور", 5, "ar");
        assertEquals(1, arabic.size());
        assertEquals(baseUrl() + "/ar/simulations/fraction-matcher", arabic.get(0).url());
        assertEquals("ar", adapter.resourceLanguage("ar"));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String baseUrlSlug(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    private static String capturedAuthHeader() {
        return OAUTH_AUTH_HEADER.get();
    }

    private static final AtomicReference<String> OAUTH_AUTH_HEADER = new AtomicReference<>();

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        if (exchange.getRequestURI().getPath().equals("/cms/pages")) {
            OAUTH_AUTH_HEADER.set(exchange.getRequestHeaders().getFirst("Authorization"));
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
