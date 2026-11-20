package tn.mathzero.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class RequestIdWebFilterTest {

    private final RequestIdWebFilter filter = new RequestIdWebFilter();
    private final WebFilterChain chain = exchange -> Mono.empty();

    @Test
    void preservesASafeIncomingRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/health")
                        .header(RequestIdWebFilter.REQUEST_ID_HEADER, "client-request-42")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders()
                .getFirst(RequestIdWebFilter.REQUEST_ID_HEADER)).isEqualTo("client-request-42");
        Object attribute = exchange.getAttribute(RequestIdWebFilter.REQUEST_ID_ATTRIBUTE);
        assertThat(attribute).isEqualTo("client-request-42");
    }

    @Test
    void generatesARequestIdWhenTheHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/health").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders()
                .getFirst(RequestIdWebFilter.REQUEST_ID_HEADER)).isNotBlank();
        Object attribute = exchange.getAttribute(RequestIdWebFilter.REQUEST_ID_ATTRIBUTE);
        assertThat(attribute).isInstanceOf(String.class);
    }

    @Test
    void rejectsUnsafeRequestIdValues() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/health")
                        .header(RequestIdWebFilter.REQUEST_ID_HEADER, "unsafe id with spaces")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders()
                .getFirst(RequestIdWebFilter.REQUEST_ID_HEADER))
                .isNotEqualTo("unsafe id with spaces");
    }
}
