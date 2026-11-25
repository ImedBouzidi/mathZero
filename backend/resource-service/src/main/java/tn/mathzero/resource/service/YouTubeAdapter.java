package tn.mathzero.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YouTubeAdapter implements ResourceProviderAdapter {
    private final boolean configured;

    public YouTubeAdapter(
            @Value("${app.providers.youtube.enabled:false}") boolean enabled,
            @Value("${app.providers.youtube.api-key:}") String apiKey) {
        this.configured = enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public ResourceProviderDescriptor descriptor() {
        return new ResourceProviderDescriptor(
                "YOUTUBE",
                "SCAFFOLDED",
                configured,
                "No provider search or automatic import is enabled; verify rights and attribution manually.");
    }
}
