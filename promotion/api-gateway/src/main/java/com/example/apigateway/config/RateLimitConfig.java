package com.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    // 이걸 사용하려면 application.yaml 파일에 default-filters 으로 등록하면 됨

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 초당 허용되는 요청 수
        // burstCapacity: 최대 누적 가능한 요청 수
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        // 요청이 들어왔을때 어떤걸 key 로 잡고 카운트 할 것인지에 대한 설정
        // 여기서는 user 별로 요청을 카운트한다.
        return exchange -> Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-ID") != null ?
                        exchange.getRequest().getHeaders().getFirst("X-User-ID") :
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
