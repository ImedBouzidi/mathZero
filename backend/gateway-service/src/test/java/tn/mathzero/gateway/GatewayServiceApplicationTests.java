package tn.mathzero.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Gateway starts without a running Eureka instance.
    }
}
