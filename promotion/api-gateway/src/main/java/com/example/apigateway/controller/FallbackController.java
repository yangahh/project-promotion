package com.example.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @RequestMapping("/users")  // 원래 요청 메서드(POST 등)가 그대로 forward 되므로 모든 메서드를 받는다
    public Mono<Map<String, Object>> userFallback() {
        // user-service가 죽었을때 핸들링 로직을 넣는다거나 다른 라우터로 등록해서 Fallback 로직을 구현할 수 있다.
        return Mono.just(Map.of("status", "down"));
    }
}
