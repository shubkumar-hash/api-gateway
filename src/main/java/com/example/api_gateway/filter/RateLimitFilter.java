package com.example.api_gateway.filter;

import com.example.api_gateway.dto.RateLimitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Skip rate limiter endpoint itself
        if (path.contains("/rate-limit")) {
            return chain.filter(exchange);
        }

        String merchantIdHeader =
                exchange.getRequest().getHeaders().getFirst("X-Merchant-Id");

        if (merchantIdHeader == null) {
            return chain.filter(exchange);
        }

        UUID merchantId;
        try {
            merchantId = UUID.fromString(merchantIdHeader);
        } catch (Exception e) {
            return chain.filter(exchange);
        }

        return webClient.get()
                .uri("http://localhost:8085/api/rate-limit/" + merchantId)
                .retrieve()
                .bodyToMono(RateLimitResponse.class)
                .flatMap(response -> {

                    if (!response.isAllowed()) {

                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

                        String body = "{\"message\":\"Rate limit exceeded\"}";
                        var buffer = exchange.getResponse()
                                .bufferFactory()
                                .wrap(body.getBytes());

                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    }

                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -50;
    }
}