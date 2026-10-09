package com.example.apigateway.filter;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    @LoadBalanced
    private final WebClient webClient;

    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        super(Config.class);
        this.webClient = WebClient.builder()
                .filter(lbFunction)
                .baseUrl("http://user-service")  // application.yml에서 gateway 부분에 id로 작성한
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {  // JwtAuthenticationFilter 필터 타고 들어오면 호출되는 메서드
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return validateToken(token)
                        .flatMap(userId -> proceedWithUserId(userId, exchange, chain))
                        .switchIfEmpty(chain.filter(exchange)) // If token is invalid, continue without setting userId
                        .onErrorResume(e -> handleAuthenticationError(exchange, e)); // Handle errors
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Long> validateToken(String token) {
        return webClient.post()
                .uri("/api/v1/users/validate-token")
                .bodyValue("{\"token\":\"" + token + "\"}")
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> Long.valueOf(response.get("id").toString()) );
    }

    private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
        // 토큰에서 가져온 userId를 X-USER-ID 헤더에 주입
        // mutate()는 새 객체를 반환하므로, 변경된 exchange를 다음 필터로 넘겨야 헤더가 실제로 전달된다
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.header("X-USER-ID", String.valueOf(userId)))
                .build();
        return chain.filter(mutatedExchange);
    }

    public static class Config {
        // 필터 구성을 위한 설정 클래스
    }
}
