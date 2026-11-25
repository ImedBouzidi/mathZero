package tn.mathzero.resource.service;

import java.util.List;

import tn.mathzero.resource.domain.ExternalResource;

public interface ResourceProviderAdapter {
    ResourceProviderDescriptor descriptor();

    default List<ProviderSearchResult> search(String query, int limit) {
        return List.of();
    }

    default List<ProviderSearchResult> search(String query, int limit, String language) {
        return search(query, limit);
    }

    record ProviderSearchResult(String title, String url, String snippet, String source) {}

    /**
     * Rights metadata recorded with an imported resource. Adapters own this
     * information: clients only send a URL from an earlier search, never a
     * licence of their choosing. A {@code null} value means the adapter does not
     * support import.
     */
    record ProviderRights(
            ExternalResource.Type type,
            String licenceCode,
            String licenceName,
            String licenceUrl,
            String attributionText,
            String attributionUrl) {}

    default ProviderRights rightsFor(ProviderSearchResult result) {
        return null;
    }

    /** Guards the import flow against a result URL from another provider/language. */
    default boolean acceptsUrl(String url, String language) {
        return true;
    }

    /** Stable provider-specific identifier for the result, capped by the database column. */
    default String externalIdFor(ProviderSearchResult result) {
        return result.url();
    }

    /** Language to store on the imported resource (some catalogues are English-only). */
    default String resourceLanguage(String selectedLanguage) {
        return selectedLanguage;
    }
}
