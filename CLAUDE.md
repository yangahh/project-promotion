# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Spring Cloud microservices study project for a "promotion" system. Stack: Java 25 (Gradle toolchain), Spring Boot 4.1.x, Spring Cloud 2025.1.x, Gradle 9 wrapper, Lombok. Code comments and config notes are partly in Korean.

## Layout

The Gradle root is `promotion/` (not the git root). It is a multi-project build:

- `promotion/` (root project) — a placeholder Spring Boot app (`PromotionApplication`) with no real logic.
- `promotion/discovery-service/` — Eureka server (`@EnableEurekaServer`), port **8761**. Does not register itself.
- `promotion/user-service/` — user/auth service, port **8004**. Eureka client pointing at `http://localhost:8761/eureka/`; Spring MVC + JPA on an in-memory H2 DB (`/h2-console` enabled, `ddl-auto: update`); JWT via jjwt 0.13 (secret in `jwt.secret`).

New services are added as subprojects via `include` in `promotion/settings.gradle`. Each subproject has its own full `build.gradle` (plugins, Java toolchain, Spring Cloud BOM) rather than sharing config from the root — follow that pattern. Subprojects also carry their own `gradlew`, but use the root wrapper.

Services discover each other through Eureka, so start `discovery-service` before any client service.

## Commands

Run from `promotion/`:

```bash
./gradlew build                                   # build + test all projects
./gradlew :user-service:build                     # build one service
./gradlew :discovery-service:bootRun              # start Eureka (run first)
./gradlew :user-service:bootRun                   # start user-service
./gradlew :user-service:test                      # tests for one service
./gradlew :user-service:test --tests 'com.example.userservice.UserServiceApplicationTests'
./gradlew :user-service:test --tests '*UserServiceApplicationTests.contextLoads'
```

Tests use JUnit 5 (`useJUnitPlatform()`); current tests are only `@SpringBootTest` context-load smoke tests.

## user-service structure

Layered packages under `com.example.userservice`: `entity` (JPA: `User` ↔ `UserLoginHistory` one-to-many), `repository` (Spring Data JPA), `service` (`UserService`: signup, authenticate, update, change password), `exception` (domain `RuntimeException`s). No controller layer, DTOs, or JWT/security config exist yet.

Known gaps in the work-in-progress user-service (it will not boot as-is):
- `UserService` injects `PasswordEncoder`; `spring-security-crypto` is only on the classpath transitively (via the Eureka client), and no `PasswordEncoder` bean is defined anywhere.
- `application.yaml` configures H2, but no `com.h2database:h2` runtime dependency is declared.
- Entity `columnDefinition`s use MySQL syntax (`ON UPDATE CURRENT_TIMESTAMP`), which H2 DDL generation may reject.
