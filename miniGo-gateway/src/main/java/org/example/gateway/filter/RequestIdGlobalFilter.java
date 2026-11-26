package org.example.gateway.filter;

import org.example.gateway.support.GatewayRequestAttributes;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Pattern SAFE_REQUEST_ID =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{7,63}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String suppliedRequestId = exchange.getRequest()
                .getHeaders()
                .getFirst(REQUEST_ID_HEADER);
        String requestId = SAFE_REQUEST_ID.matcher(
                suppliedRequestId == null ? "" : suppliedRequestId
        ).matches()
                ? suppliedRequestId
                : UUID.randomUUID().toString().replace("-", "");

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(REQUEST_ID_HEADER, requestId))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getAttributes().put(GatewayRequestAttributes.REQUEST_ID, requestId);
        mutatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
