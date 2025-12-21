package com.example.server.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class LoggingWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant startTime = Instant.now();
        String requestId = generateRequestId();
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().value();
        String clientIp = getClientIp(exchange);

        log.info("║ [REQUEST-{}] {} {} from {} - Headers: {}",
                requestId,
                method,
                path,
                clientIp,
                exchange.getRequest().getHeaders().toSingleValueMap());

        exchange.getResponse().getHeaders().add("X-Request-ID", requestId);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);
                    int statusCode = exchange.getResponse().getStatusCode() != null ?
                            exchange.getResponse().getStatusCode().value() : 0;

                    log.info("║ [RESPONSE-{}] {} {} - Status: {} - Duration: {} ms - Signal: {}",
                            requestId,
                            method,
                            path,
                            statusCode,
                            duration.toMillis(),
                            signalType);
                });
    }

    private String generateRequestId() {
        return String.format("%08x", System.nanoTime());
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }
}
