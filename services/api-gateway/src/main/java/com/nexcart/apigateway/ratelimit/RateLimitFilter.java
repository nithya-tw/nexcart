package com.nexcart.apigateway.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitFilter(RateLimiterRegistry rateLimiterRegistry) {
        super(Config.class);
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String key = userId != null ? userId : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(key);
            
            return Mono.fromCallable(() -> rateLimiter.acquirePermission())
                .flatMap(permitted -> {
                    if (permitted) {
                        return chain.filter(exchange);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                })
                .transformDeferred(RateLimiterOperator.of(rateLimiter));
        };
    }

    public static class Config {
    }
}
