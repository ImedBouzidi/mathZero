package tn.mathzero.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CnteAdapter implements ResourceProviderAdapter {
    private final boolean authorized;
    private final boolean configured;

    public CnteAdapter(
            @Value("${app.providers.cnte.authorized:false}") boolean authorized,
            @Value("${app.providers.cnte.base-url:}") String baseUrl,
            @Value("${app.providers.cnte.api-token:}") String apiToken) {
        this.authorized = authorized;
        this.configured = baseUrl != null && !baseUrl.isBlank()
                && apiToken != null && !apiToken.isBlank();
    }

    @Override
    public ResourceProviderDescriptor descriptor() {
        if (!authorized) {
            return new ResourceProviderDescriptor(
                    "CNTE", "BLOCKED_AUTHORIZATION", false,
                    "Access remains disabled until CNTE authorization and an API contract are supplied.");
        }
        return new ResourceProviderDescriptor(
                "CNTE",
                "SCAFFOLDED",
                configured,
                "No CNTE API requests are made; an approved endpoint contract is required before integration.");
    }
}
