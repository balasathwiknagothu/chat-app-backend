# Decision Log

Every major engineering decision made in this project, the alternatives considered, and why the chosen option won.

---

### 1. Unify direct messages and group chats under one `Room` model
**Alternatives:** Separate `DirectMessage` and `GroupMessage` tables/entities.
**Why chosen:** Every feature (pagination, history, broadcast, future read receipts) gets built once and works for both. Matches the industry-standard pattern used by Slack/Discord/Teams internally.
**Trade-off accepted:** `Room.name` must be nullable, since a direct room doesn't need one — a minor schema looseness in exchange for not duplicating every feature.

### 2. `BIGINT AUTO_INCREMENT` primary keys, not UUID
**Alternatives:** UUID (v4 or time-ordered UUIDv7/ULID).
**Why chosen:** Single MySQL instance, monolith architecture — no distributed ID generation problem to solve. Sequential IDs give better B-tree index performance on the highest-write table (`Message`).
**Trade-off accepted:** IDs are sequential/guessable — acceptable since we never expose raw entity IDs as sensitive identifiers, and this isn't a distributed system.

### 3. `RoomMembership` as an explicit join entity, not a bare `@ManyToMany`
**Alternatives:** JPA's implicit `@ManyToMany` mapping.
**Why chosen:** Needed `joinedAt` on the relationship itself from day one, and JPA forces a refactor to an explicit join entity the moment you need any data on the relationship — better to start there.

### 4. BCrypt for password hashing
**Alternatives:** SHA-256 or other general-purpose hash functions.
**Why chosen:** Password hashing needs to be deliberately slow to resist brute-forcing; general-purpose hashes are built for speed, which is the opposite of what's needed here. BCrypt also handles salting automatically.

### 5. Stateless JWT authentication, not server-side sessions
**Alternatives:** Traditional `HttpSession`-based auth.
**Why chosen:** No server-side state to store or share; critical because the same identity needs to authenticate both REST requests and a long-lived WebSocket connection, and sharing an HTTP session with a WebSocket connection is awkward. JWT in headers (HTTP) and STOMP `CONNECT` headers (WebSocket) is a consistent mechanism across both.

### 6. STOMP over raw WebSocket
**Alternatives:** Raw WebSocket with a hand-rolled message envelope/routing scheme.
**Why chosen:** STOMP provides pub/sub semantics (topics, queues, subscriptions) out of the box — avoids manually tracking "who's subscribed to what" and routing messages ourselves.

### 7. Spring's Simple Broker, not an external broker (RabbitMQ)
**Alternatives:** STOMP relay to an external message broker.
**Why chosen:** Matches the deliberate single-instance monolith scope from the project's initial architecture decision (no microservices, no Docker).
**Trade-off accepted:** Won't work correctly across multiple server instances — an in-memory broker on Server A has no visibility into WebSocket sessions on Server B. Documented as the first thing to change if this needed to scale horizontally.

### 8. `EnumType.STRING` for `Room.type`, not `EnumType.ORDINAL`
**Alternatives:** Ordinal (integer-based) enum storage, or leaving it as a plain validated `String`.
**Why chosen:** Ordinal storage silently corrupts data if enum values are ever reordered or inserted mid-list; STRING is human-readable in the database and immune to that failure mode. (Note: the project initially shipped with a plain `String` + `@Pattern` validation as an interim measure, upgraded to the proper enum in the Phase 7 code review — see Mistakes to Avoid.)

### 9. IP-based rate limiting on auth endpoints, not username-based
**Alternatives:** Rate limit by username/account.
**Why chosen:** At the point rate limiting must run (before authentication executes), the system doesn't yet know if the submitted username is even valid — IP is the only identity available at that layer.

### 10. `ddl-auto=update` instead of a migration tool (Flyway/Liquibase)
**Alternatives:** Flyway or Liquibase migrations from day one.
**Why chosen:** Faster iteration during active development of a project still being designed step-by-step.
**Trade-off accepted:** Not production-safe — schema changes aren't tracked/versioned, and some changes (like the `Room.type` enum conversion in Phase 7) required manually dropping tables. Explicitly flagged as a pre-production requirement, not adopted in this project's scope.

### 11. DTOs at every API and WebSocket boundary, never raw entities
**Alternatives:** Returning JPA entities directly from controllers/STOMP handlers.
**Why chosen:** Prevents accidental serialization of sensitive fields (password hashes) and lazy-loaded associations that may not be initialized outside a Hibernate session (`LazyInitializationException` risk).

### 12. Postman for REST testing, a custom browser-based STOMP client for WebSocket testing
**Alternatives:** Postman's raw WebSocket support with manual STOMP frame construction.
**Why chosen:** Postman has solid native WebSocket support but doesn't parse/build STOMP frames — testing real STOMP behavior is cleaner with a small dedicated HTML/JS test client using `stomp.js`/`sockjs-client`.
