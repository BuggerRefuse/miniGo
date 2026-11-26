package org.example.gateway.filter;

import org.example.gateway.support.GatewayRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

@Component
public class GatewayAccessLogGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(GatewayAccessLogGlobalFilter.class);
    private static final String UNKNOWN = "unknown";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startedAt = System.nanoTime();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String requestId = attribute(exchange, GatewayRequestAttributes.REQUEST_ID);
        String clientIp = resolveClientIp(exchange);

        return chain.filter(exchange).doFinally(signal -> {
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            long durationMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            log.info(
                    "gateway_access method={} path={} status={} duration_ms={} "
                            + "request_id={} client_ip={} user_id={} signal={}",
                    method,
                    path,
                    statusCode == null ? "CLIENT_CLOSED" : statusCode.value(),
                    durationMs,
                    requestId,
                    clientIp,
                    attribute(exchange, GatewayRequestAttributes.USER_ID),
                    signal
            );
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN;
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private String attribute(ServerWebExchange exchange, String name) {
        Object value = exchange.getAttributes().get(name);
        return value == null ? UNKNOWN : String.valueOf(value);
    }
}
