# Engineering Notes

Phase-by-phase record of what was built, why, and the core concepts involved.

---

## Phase 1 — Planning, Database Design, Project Setup, User Entity

**What we built:** Requirements analysis, ERD (User/Room/RoomMembership/Message), Spring Boot project skeleton, layered package structure, `User` entity + repository.

**Key concepts:**
- Functional vs non-functional requirements as a planning discipline
- Unifying direct and group chat under one `Room` abstraction (see Decision Log)
- `BIGINT AUTO_INCREMENT` vs UUID primary keys — chosen for monolith + single-DB-instance context
- 3NF normalization; avoiding repeating-group anti-patterns (e.g., no comma-separated member lists)
- `@PrePersist` for default field values (`createdAt`, `status`) at the entity level, guaranteed to run at insert time

**Best practices applied:** naming the `users` table explicitly (avoiding MySQL's reserved `user` word), composite indexes designed around actual query patterns before writing any code.

---

## Phase 2 — Authentication Foundation

**What we built:** BCrypt password hashing, registration API, JWT generation/validation utility, login API, Spring Security stateless filter chain, global exception handling foundation.

**Key concepts:**
- BCrypt's deliberate slowness as a security property, vs fast general-purpose hashes
- JWT structure (header.payload.signature) and why the payload is *readable but not forgeable*
- Stateless auth (`SessionCreationPolicy.STATELESS`) — no server-side session state, which matters doubly because WebSocket connections need to carry their own auth
- `OncePerRequestFilter` for the JWT filter — guarantees exactly one execution per request even across internal dispatch
- Constructor injection (`@RequiredArgsConstructor`) over field injection — testability and explicit dependencies
- Centralized exception handling via `@RestControllerAdvice` — a cross-cutting concern handled once, not per-controller

**Best practices applied:** identical error message for "user not found" and "wrong password" at login, to prevent username enumeration.

---

## Phase 3 — Rooms, Membership, WebSocket/STOMP Foundation

**What we built:** `Room`/`RoomMembership` entities, full WebSocket/STOMP configuration (Simple Broker, heartbeats, SockJS), JWT authentication at the STOMP `CONNECT` frame level via a custom `ChannelInterceptor`.

**Key concepts:**
- HTTP (request/response) vs WebSocket (persistent, full-duplex) — and why chat specifically needs full-duplex, ruling out SSE (server→client only)
- The WebSocket handshake as an HTTP `101 Switching Protocols` upgrade — meaning the handshake URL still needs to pass through Spring Security's HTTP filter chain
- STOMP as pub/sub semantics layered on raw WebSocket frames (topics, queues) — avoids hand-rolling message routing
- Simple Broker (in-memory) vs external broker (RabbitMQ via STOMP relay) — Simple Broker chosen for single-instance monolith; would need to swap for horizontal scaling
- STOMP-level auth requires a `ChannelInterceptor`, distinct from the HTTP-level `JwtAuthFilter` — different message pipelines
- `ThreadPoolTaskScheduler` required explicitly when configuring STOMP heartbeats — a genuinely non-obvious Spring requirement

---

## Phase 4 — Messaging, Persistence, Chat History

**What we built:** `Message` entity, `@MessageMapping`-based STOMP send handler, `SimpMessagingTemplate` broadcast, paginated REST history endpoint, (added as necessary infrastructure) Room creation/join REST endpoints.

**Key concepts:**
- Persist-then-broadcast ordering — guarantees a message is never shown as delivered before it's actually saved
- Server-side room membership verification on every send and every history read — never trust `roomId` from client input alone
- `Page<T>`/`Pageable` for chat history — translates to `LIMIT`/`OFFSET` SQL, backed by the `(room_id, sent_at)` composite index designed in Phase 1
- DTOs at every message boundary (`ChatMessageRequest`/`Response`) to avoid serializing lazy-loaded JPA associations outside a Hibernate session

---

## Phase 5 — Presence, Validation, Exception Handling Polish

**What we built:** `SessionConnectedEvent`/`SessionDisconnectEvent` listeners updating user status, Bean Validation on request DTOs, expanded `GlobalExceptionHandler` coverage.

**Key concepts:**
- Group messaging required zero new code — direct payoff of the unified Room model from Phase 1
- Spring's generic `@EventListener` mechanism (not WebSocket-specific) as the only reliable hook for "connection ended," since disconnects can happen many ways (clean close, dropped wifi, sleep) that a manually-called method can't cover
- `@Pattern` validation as an interim type-safety guard before a real enum was introduced (Phase 7 fix)
- Checking resource existence *before* checking authorization — otherwise a 404 case gets misreported as 403

---

## Phase 6 — Logging, API Documentation, Rate Limiting

**What we built:** SLF4J/Logback logging via Lombok's `@Slf4j` at key service and exception-handling points, Swagger/OpenAPI documentation, IP-based rate limiting on auth endpoints via Bucket4j.

**Key concepts:**
- SLF4J as a facade over Logback (Facade pattern) — same reasoning as JDBC over a specific driver
- `WARN` for expected client-side failures vs `ERROR` (with full stack trace) reserved for genuinely unexpected exceptions
- Never logging credentials, even on failed login attempts — log *who* and *that it failed*, never the password
- OpenAPI/Swagger documents REST only — WebSocket/STOMP has no equivalent standard, documented separately in this project's own docs
- Token bucket rate limiting, keyed by IP (not username) because identity isn't known yet at the point rate limiting must run — before authentication logic executes

---

## Phase 7 — Testing, Code Review, Git, GitHub

**What we built:** Mockito-based unit tests for `AuthService`, three code-review fixes (password hash leak in register response, missing authorization on `addMember`, `Room.type` upgraded from `String` to a proper `RoomType` enum), Git history, GitHub push.

**Key concepts:**
- Testing pyramid reasoning: unit-test the service layer (business logic) with mocked repositories; controllers stay thin and were already integration-tested manually throughout the project via Postman/browser
- `@Mock` + `@InjectMocks` + `verify(...)` — asserting both return values and side effects (e.g., "save was never called")
- `EnumType.STRING` over the default `EnumType.ORDINAL` — avoids silent data corruption if enum values are ever reordered
- A real code review pass: revisiting flagged gaps from earlier phases with fresh eyes, rather than treating "it works" as "it's done"
