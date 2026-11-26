package org.example.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdGlobalFilterTest {

    private final RequestIdGlobalFilter filter = new RequestIdGlobalFilter();

    @Test
    void preservesSafeClientRequestId() {
        String requestId = "client-request-1234";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/login.html")
                        .header(RequestIdGlobalFilter.REQUEST_ID_HEADER, requestId)
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, forwardedExchange -> {
                    forwarded.set(forwardedExchange);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo(requestId);
        assertThat(exchange.getResponse().getHeaders()
                .getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo(requestId);
    }

    @Test
    void replacesUnsafeClientRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/login.html")
                        .header(
                                RequestIdGlobalFilter.REQUEST_ID_HEADER,
                                "<script>alert(1)</script>"
                        )
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, forwardedExchange -> {
                    forwarded.set(forwardedExchange);
                    return Mono.empty();
                }))
                .verifyComplete();

        String generatedId = forwarded.get().getRequest().getHeaders()
                .getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER);
        assertThat(generatedId)
                .hasSize(32)
                .doesNotContain("<script>");
    }
}
