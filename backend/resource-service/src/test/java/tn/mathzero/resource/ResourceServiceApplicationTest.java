package tn.mathzero.resource;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tn.mathzero.resource.service.ResourceService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:resource;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "eureka.client.enabled=false"
})
class ResourceServiceApplicationTest {
    @Autowired private ResourceService resourceService;

    @Test
    void migrationsMatchTheResourceEntitiesAndProviderScaffoldsArePresent() {
        Set<String> providers = resourceService.providerStatus().stream()
                .map(provider -> provider.provider())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(providers.containsAll(Set.of(
                "MANUAL", "WIKIMEDIA", "OPENSTAX", "GEOGEBRA", "PHET", "YOUTUBE", "CNTE")));
    }
}
