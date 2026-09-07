# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Strict Task Scope and Change Control

The agent must strictly follow the scope of the current requirement/task. Only modify existing files, code, configuration, dependencies, documentation, or other project resources that are directly necessary to complete the requested task.

Before making any changes:

1. Identify the exact requirement being implemented. If requirements are ambiguous, ask for clarification before implementing rather than making assumptions.
2. Identify the minimum set of files that need to be modified.
3. Do not modify any file outside this scope unless it is explicitly required to complete the task.
4. Do not create new files unless creating them is explicitly required by the task. Before creating a new file, verify whether the requirement can be fulfilled by modifying an existing file.
5. Do not rename, move, delete, reorganize, refactor, optimize, or rewrite code unless explicitly required by the task.
6. Do not reformat code, reorder imports, change whitespace, or delete/modify existing comments and docstrings outside the edited lines. Respect established language conventions (Vietnamese comments/logs).
7. Do not fix unrelated bugs, code smells, formatting issues, technical debt, or architectural problems.
8. Do not add, remove, upgrade, downgrade, or replace dependencies (e.g. via `npm install`, `npm audit fix`, or POM edits) unless explicitly required.
9. Do not modify environment variables, configuration, build scripts, CI/CD, Docker/compose, deployment, database schema, API contracts, interfaces, authentication, authorization, or business logic unless directly required.
10. Never modify or weaken existing test assertions just to make tests pass.
11. Do not perform git operations (`commit`, `push`, `stash`, `checkout`) or destructive system/database actions unless explicitly instructed.
12. Preserve all existing behavior outside the requested requirement.

If an unrelated issue is discovered, do not fix it. Report it separately to the user.

If completing the task appears to require modifying files or components outside the originally identified scope, STOP before making those changes. Explain why the additional changes are necessary and request explicit approval before proceeding.

After completing the task:
- Verify changes by running only the targeted test or compile check directly related to the modification (e.g., specific `mvn test -Dtest=...` or `tsc`).
- Do not perform additional cleanup, refactoring, optimization, formatting, or architectural improvements that were not explicitly requested.

Follow the Minimal Change Principle: make the smallest possible change that completely satisfies the requirement while preserving all unrelated existing behavior.

## Overview

TravelK — a bus ticket booking system. Two independent projects in one repo:

- `backend/` — Spring Boot 3.5.3 / Java 21 REST API (Maven, package `com.khaiquang`, entrypoint `Main.java`)
- `frontend/` — React 18 + TypeScript SPA built with Vite, served by Nginx in production

Infrastructure (MySQL, Redis, RabbitMQ, Prometheus, Grafana) is wired up by the root `docker-compose.yml`.

Code comments, log messages, and user-facing error strings are largely in Vietnamese — keep that convention when editing existing files.

## Commands

Backend (run from `backend/`):

```sh
mvn clean verify              # compile + run all tests
mvn spring-boot:run           # run the API on :8080
mvn test -Dtest=BookingServiceImplTest              # single test class
mvn test -Dtest=BookingServiceImplTest#methodName   # single test method
mvn clean package -DskipTests
```

Frontend (run from `frontend/`):

```sh
npm install
npm run dev      # Vite dev server on :3000, proxies /api -> 127.0.0.1:8080
npm run build    # tsc type-check + vite build
```

Full stack:

```sh
docker-compose up -d --build
# frontend :3000, backend :8080, RabbitMQ UI :15672, Prometheus :9090, Grafana :3001
```

There is no linter configured for either project. Type errors are only caught by `tsc` during `npm run build`.

## Required environment

`backend/.env` (gitignored, loaded by docker-compose via `env_file`) must define `MAIL_USERNAME`, `MAIL_PASSWORD`, `GEMINI_API_KEY`. These have no defaults in `application.properties`, so the app fails to start without them. Every other external host (`SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, `SPRING_RABBITMQ_HOST`, …) has a localhost default, so running `mvn spring-boot:run` against locally-running containers works without extra config.

Schema is created by Hibernate (`spring.jpa.hibernate.ddl-auto=update`); `backend/docker/mysql/init.sql` only creates the empty database. `DataInitializer` (CommandLineRunner) seeds roles and a default admin on every boot.

## Architecture

### Backend layering

`controller → service (interface) → service.impl → repository (Spring Data JPA) → entity`

Every service has an interface in `service/` and an implementation in `service/impl/`. Controllers depend on the interface, use constructor injection via Lombok `@RequiredArgsConstructor`, and return `ResponseEntity<...>` directly. DTOs are split into `dto/request`, `dto/response`, `dto/message` (RabbitMQ payloads), with MapStruct mappers in `dto/mapper`. Errors are translated by `exception/GlobalExceptionHandler` using `ResourceNotFoundException` / `ResourceDuplicateException` / `BusAPIException`.

### Seat concurrency (the core design constraint)

Seat holds are Redis distributed locks, not DB rows. Key format `hold:trip:{tripId}:seat:{seatNumber}`, value = userId, acquired with `SET NX` + 10-minute TTL, released with a Lua compare-and-delete script so a user can only release their own lock. All of this lives in `BookingServiceImpl` (not `SeatServiceImpl` — that one only reads the static seat layout of a bus).

Three layers protect against double-booking, and all three must stay in sync when touching booking code:

1. `holdSeat`/`releaseSeat` — transient locks while the user is picking seats on the seat map.
2. `createBooking` — re-acquires locks for all requested seats, then re-checks `ticketRepository.findByTripIdAndSeatNumberIn` for already-sold seats before persisting, then releases the locks.
3. `autoCancelUnpaidBookings` — `@Scheduled(fixedRate = 60000)`, deletes tickets and flips bookings to `CANCELLED` if still `PENDING` after 5 minutes. Scheduling is enabled by `@EnableScheduling` on `Main`.

Booking status flow: `PENDING` (created) → paid via VNPay callback, or `CANCELLED` by the scheduler.

### Payment + async email

`PaymentServiceImpl` builds a signed VNPay sandbox redirect URL (config in `application.properties` under `vnp_*`, return URL `/api/payment/vnpay-return`). On a successful return it publishes an `OrderEvent` to RabbitMQ (`order_exchange` / `order_routing_key` / `order_queue`, JSON converter). `consumer/OrderConsumer` picks it up and renders a FreeMarker template from `resources/templates/` to send the ticket email — so email is never on the request path.

### Auth

Stateless JWT. `JwtFilter` reads `Authorization: Bearer`, validates via `JwtProvider`, loads the user through `CustomUserDetailService`, and populates the SecurityContext. `SecurityConfig` currently `permitAll()`s nearly every `/api/**` path; authorization is instead enforced per-endpoint with `@PreAuthorize("hasRole('ADMIN')")` (`@EnableMethodSecurity` is on). If you add an admin endpoint, the `@PreAuthorize` annotation is what protects it — do not assume the filter chain does.

### Redis is used for two things

Distributed locking (above) and Spring Cache (`@EnableCaching`, 10-minute default TTL, JSON serializer with default typing enabled in `RedisConfig`). Cached values embed `@class` type info, so renaming or moving a cached DTO class invalidates existing entries at deserialization time.

### Chatbot

`ChatServiceImpl` does hand-rolled RAG: keyword-matches the user message against a few hardcoded Vietnamese city names, queries `TripRepository` for matching trips, formats them as text, and injects them as `{tripData}` into the system prompt at `resources/prompts/travelk-chatbot-instruction.st` before calling Gemini through Spring AI. Failures are swallowed and returned as a Vietnamese fallback string.

### Frontend

No state library and no API client layer — components call `axios` directly and read the JWT/user from `localStorage`. Routing lives entirely in `src/App.tsx`: public pages from `src/components/`, admin pages nested under `/admin` from `src/admin/` behind `AdminLayout`. Tailwind is loaded from the CDN in `index.html`, so there is no Tailwind build step or config file — custom Tailwind config/plugins are not available.

In dev, Vite proxies `/api` to `127.0.0.1:8080`. In Docker, `frontend/nginx.conf` proxies `/api` to `http://bus_app:8080`. Requests are always relative paths; do not hardcode the backend origin.

## Testing

Tests are pure Mockito unit tests (`@ExtendWith(MockitoExtension.class)`) in `backend/src/test/java/com/khaiquang/service/` — no Spring context is started. `src/test/resources/application-test.properties` describes an H2 setup, but the H2 dependency is not in `pom.xml`; there are currently no integration tests.

## Known repo quirks

- The GitHub Actions workflow is at `backend/.github/workflows/`, not the repo root, so GitHub does not pick it up as-is. It also runs `mvn` and reads `target/surefire-reports/` from the checkout root, while the POM lives in `backend/`.
- `backend/monitoring/prometheus.yml` duplicates `backend/docker/monitoring/prometheus.yml`; docker-compose mounts the `docker/` one.
- The VNPay sandbox credentials and `app.jwtSecret` are committed in `application.properties`.
