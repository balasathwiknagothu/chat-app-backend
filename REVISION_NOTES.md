# Revision Notes — Quick Reference

Fast recall summary. Use this the night before an interview.

---

## The 30-Second Project Summary
Real-time chat backend in Java 21 / Spring Boot 3.5. JWT-secured REST API for auth/rooms/history, WebSocket/STOMP for live messaging, MySQL persistence, rate-limited and fully documented (Swagger). Direct and group chat share one unified `Room` data model. Tested with JUnit/Mockito, deployed via Git to GitHub.

## Core Entities
`User` — `Room` (type: DIRECT/GROUP) — `RoomMembership` (join table) — `Message` (indexed on `room_id + sent_at`)

## Key Numbers
- JWT expiry: 24 hours
- Message content cap: 5000 characters
- Rate limit: 5 requests/minute/IP on auth endpoints
- STOMP heartbeat: 10 seconds both directions
- Pagination default: 20 messages/page, zero-indexed

## One-Liners for Common Questions

| Question | One-liner |
|---|---|
| Why unify direct/group chat? | One data model, one set of features, matches Slack/Discord's internal pattern |
| Why BIGINT not UUID? | Single DB instance, no distributed ID problem, better write index performance |
| Why JWT not sessions? | Stateless; same mechanism secures both REST and WebSocket |
| Why BCrypt not SHA-256? | Password hashing needs to be slow on purpose |
| Why STOMP not raw WebSocket? | Pub/sub routing built in, no hand-rolled message envelope |
| Why Simple Broker not RabbitMQ? | Matches single-instance monolith scope; documented scaling limitation |
| Why DTOs everywhere? | Prevent leaking entities (password hash) and lazy-load crashes |
| Why rate limit by IP not username? | Username isn't verified yet at that point in the pipeline |
| Why EnumType.STRING not ORDINAL? | Ordinal silently breaks if enum order ever changes |

## The Filter Chain (in order)
1. `RateLimitFilter` — blocks over-limit requests before anything else runs
2. `JwtAuthFilter` — validates bearer token, sets `SecurityContext`
3. Spring's own filters (`UsernamePasswordAuthenticationFilter`, etc.)

## The STOMP Auth Flow (different from REST)
1. HTTP handshake → `101 Switching Protocols` (passes through normal HTTP filter chain, `permitAll()`'d)
2. First STOMP frame after upgrade: `CONNECT`, carries `Authorization: Bearer <token>` as a **STOMP header**, not HTTP header
3. `StompAuthChannelInterceptor.preSend()` validates it, attaches `Principal` to the session
4. Every subsequent `SEND`/`SUBSCRIBE` frame on that session now carries identity

## Message Send Flow
Client → `/app/chat.send` → `ChatController.@MessageMapping` → `MessageService.sendMessage()` (validates membership, persists via repository) → `SimpMessagingTemplate.convertAndSend("/topic/room.{id}", response)` → all subscribers receive it

## Authorization Pattern (applied everywhere)
Never trust client-supplied IDs. Every service method that touches a room checks: does the room exist? → is the requester a member? → then proceed. Applied consistently to send, history read, and add-member (the last one was a code-review fix, not there from day one — good story to tell about catching your own gaps).

## Testing Approach
Unit test the service layer with Mockito mocks (`@Mock`, `@InjectMocks`) — fast, isolated, no real DB. Controllers stayed thin and were integration-tested manually via Postman/browser throughout every phase instead.

## Known Limitations (say these proactively, shows maturity)
- No read receipts / typing indicators (explicitly out of MVP scope)
- `ddl-auto=update`, not a real migration tool — fine for dev, not production
- Simple Broker won't survive horizontal scaling without swapping to an external broker
- Swagger documents REST only; WebSocket/STOMP documented separately in this project's own notes
