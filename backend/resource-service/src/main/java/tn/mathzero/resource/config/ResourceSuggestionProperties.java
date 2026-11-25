package tn.mathzero.resource.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import tn.mathzero.resource.domain.ExternalResource;

/** Query sets used to pre-sort provider candidates into experience cycles. */
@Component
@ConfigurationProperties(prefix = "app.suggestions")
public class ResourceSuggestionProperties {
    private Duration cacheTtl = Duration.ofMinutes(15);
    private int limitPerQuery = 5;
    private int maxPerCycle = 10;
    private Map<String, ProviderQueries> providers = new LinkedHashMap<>();

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getLimitPerQuery() {
        return limitPerQuery;
    }

    public void setLimitPerQuery(int limitPerQuery) {
        this.limitPerQuery = limitPerQuery;
    }

    public int getMaxPerCycle() {
        return maxPerCycle;
    }

    public void setMaxPerCycle(int maxPerCycle) {
        this.maxPerCycle = maxPerCycle;
    }

    public Map<String, ProviderQueries> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderQueries> providers) {
        this.providers = providers == null ? new LinkedHashMap<>() : providers;
    }

    /** Topic queries for one provider, keyed by cycle and search language. */
    public static class ProviderQueries {
        private Map<ExternalResource.Cycle, Map<String, List<String>>> queries = Map.of();

        public ProviderQueries() {}

        public ProviderQueries(Map<ExternalResource.Cycle, Map<String, List<String>>> queries) {
            this.queries = queries == null ? Map.of() : queries;
        }

        public Map<ExternalResource.Cycle, Map<String, List<String>>> getQueries() {
            return queries;
        }

        public void setQueries(Map<ExternalResource.Cycle, Map<String, List<String>>> queries) {
            this.queries = queries == null ? Map.of() : queries;
        }
    }
}
