package tn.mathzero.gateway.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import tn.mathzero.gateway.api.ApiResponse;
import tn.mathzero.gateway.web.RequestIdWebFilter;

class HealthControllerTest {

    private final HealthController controller = new HealthController("0.1.0");

    @Test
    void returnsUpStatusAndGatewayIdentity() {
        MockServerWebExchange exchange = exchangeWithRequestId("test-request-id");

        ApiResponse<GatewayHealth> response = controller.health(exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.data().status()).isEqualTo("UP");
        assertThat(response.data().service()).isEqualTo("gateway-service");
        assertThat(response.data().version()).isEqualTo("0.1.0");
        assertThat(response.data().timestamp()).isNotNull();
        assertThat(response.meta().requestId()).isEqualTo("test-request-id");
    }

    @Test
    void generatesCorrelationIdWhenTheFilterDidNotProvideOne() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/health").build());

        ApiResponse<GatewayHealth> response = controller.health(exchange).block();

        assertThat(response).isNotNull();
        assertThat(response.meta().requestId()).isNotBlank();
    }

    private static MockServerWebExchange exchangeWithRequestId(String requestId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/health").build());
        exchange.getAttributes().put(RequestIdWebFilter.REQUEST_ID_ATTRIBUTE, requestId);
        return exchange;
    }
}
