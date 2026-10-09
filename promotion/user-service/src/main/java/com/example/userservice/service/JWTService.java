package com.example.userservice.service;

import com.example.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class JWTService {
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateToken(User user) {
        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())  // gateway가 X-USER-ID 헤더로 전달할 유저 id
                .claim("role", "USER")  // TODO: User에 Role 추가
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000)) // Token expires in 1 hour
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))  // secretKey로 암호화 진행
                .compact();  // String으로 변환
    }

    public Claims validateToken(String token) {
        try {
            return parseJwtClaims(token);
        } catch (Exception e) {
            log.error("Token validation error: ", e);
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public Claims parseJwtClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String refreshToken(String token) {
        // 기존 토큰을 가지고 새 토큰 만들기 (1시간 연장)
        Claims claims = parseJwtClaims(token);
        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(claims.getSubject())
                .claims(claims)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

}
