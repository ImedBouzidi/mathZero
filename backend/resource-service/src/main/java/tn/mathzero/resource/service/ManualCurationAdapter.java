package tn.mathzero.resource.service;

import org.springframework.stereotype.Component;

@Component
public class ManualCurationAdapter implements ResourceProviderAdapter {
    @Override
    public ResourceProviderDescriptor descriptor() {
        return new ResourceProviderDescriptor(
                "MANUAL", "READY", true, "Resources are entered and rights-checked by an administrator.");
    }
}
