# Mistakes to Avoid

Every real bug hit during this project, its root cause, and the lesson — not hypothetical mistakes, things that actually happened and were debugged.

---

### 1. Package declaration must match folder path exactly
**What happened:** `AuthService.java` accidentally created inside the `security` folder while its `package` line said `com.chatapp.backend.service`. Produced ~9 cascading "cannot find symbol" errors that looked unrelated to the real cause.
**Lesson:** In Java, the folder path must exactly mirror the package declaration. One misplaced file can cascade into dozens of confusing downstream errors — always check the *first* error's file path before reading the rest.

### 2. A corrupted file can cause the same cascade as a misplaced one
**What happened:** `UserRepository.java` existed at the correct path but had corrupted/wrong content — 18 errors, all fallout from one root cause ("bad source file... does not contain class UserRepository").
**Lesson:** "Cannot access" + "bad source file" errors mean the compiler can't even parse the file as declared — always inspect that specific file's actual contents before assuming the error list represents many separate bugs.

### 3. Never paste inline instructional comments as literal code
**What happened:** An instruction like `// add this line before X` got pasted literally into `SecurityConfig.java`, including a stray method-chain line sitting outside any method body — a straight syntax error.
**Lesson:** When following written instructions with comment markers, always ask for (or write) the complete file rather than assembling fragments — fragments are for explanation, not for pasting verbatim.

### 4. STOMP heartbeats require an explicit `TaskScheduler`
**What happened:** `enableSimpleBroker(...).setHeartbeatValue(...)` alone threw `IllegalArgumentException: Heartbeat values configured but no TaskScheduler provided` at startup.
**Lesson:** Spring doesn't infer which thread pool should run a periodic heartbeat — it must be constructed and wired explicitly via `.setTaskScheduler(...)`, including calling `.initialize()` on a manually-created `ThreadPoolTaskScheduler`.

### 5. `addFilterBefore` requires the reference filter to already be registered
**What happened:** `.addFilterBefore(rateLimitFilter, JwtAuthFilter.class)` was called *before* `jwtAuthFilter` itself had been added to the chain — Spring had no idea where `JwtAuthFilter` sat yet, so it couldn't resolve "before" relative to it. Error: `"The Filter class JwtAuthFilter does not have a registered order"`.
**Lesson:** When chaining `addFilterBefore`/`addFilterAfter` calls with custom filter classes, register filters in dependency order — the filter you're positioning *relative to* must already be in the chain.

### 6. Third-party library versions must match the Spring Boot version
**What happened:** `bucket4j_jdk17-core` (wrong artifact ID) failed dependency resolution entirely; `springdoc-openapi-starter-webmvc-ui:2.6.0` compiled and started fine but threw `NoSuchMethodError` on `ControllerAdviceBean` at runtime — an internal Spring Framework method signature had changed between the versions springdoc 2.6.0 expected and what Spring Boot 3.5.16 actually shipped.
**Lesson:** A clean build and startup doesn't guarantee compatibility — some incompatibilities only surface when a specific code path executes at runtime. Always check a library's documented compatibility matrix against your exact Spring Boot version, especially for tightly-coupled libraries like springdoc that reach into Spring internals.

### 7. A `403` from your own app can look identical to an external network block
**What happened:** Swagger UI returned a browser error page reading "Access denied" — initially suspected to be a Windows/network-level content filter, since both Postman and two different browsers showed the same wording on both `localhost` and `127.0.0.1`.
**Lesson:** This particular case turned out to be the app returning `403` because the Swagger path wasn't `permitAll()`'d — always re-verify the simplest explanation (missing security rule) before escalating to environment-level theories, and use the actual response body/status code as the primary signal, not just how a page visually renders.

### 8. `public` class filenames must match; non-public classes don't enforce this
**What happened:** A correctly-written test class (`class AuthServiceTest`, no `public` modifier) was saved under the filename `AuthService.java` and compiled/ran successfully anyway — the mismatch went undetected until a `git status` review surfaced it.
**Lesson:** Java only enforces filename-matches-class-name for `public` classes. Don't rely on a successful build alone to catch filename/class mismatches — a quick visual check of `git status` before committing catches this kind of thing.

### 9. Test files must live under `src/test`, not `src/main`
**What happened:** A JUnit/Mockito test class was created under `src/main/java/.../service/` instead of `src/test/java/.../service/` — since testing libraries are only on the test classpath, this produced 22 "package does not exist" errors for `org.junit`, `org.mockito`, `org.assertj`.
**Lesson:** Before creating a test file, explicitly confirm the folder breadcrumb includes `test`, not `main` — the two folder trees mirror each other closely enough to misclick between them.

### 10. Check for accidental duplicate files after heavy debugging sessions
**What happened:** After multiple rounds of fixing file-location bugs, three real production service files (`AuthService.java`, `MessageService.java`, `RoomService.java`) ended up duplicated — full copies sitting uselessly inside `src/test/.../service/` alongside the real ones in `src/main`.
**Lesson:** After a debugging session involving several file moves/recreations, do a quick `git status` review before committing — stray duplicate files are easy to miss and easy to catch this way.

### 11. Schema changes to an existing column type may need a manual table reset in dev
**What happened:** Changing `Room.type` from `String` to a `RoomType` enum (`EnumType.STRING`) required manually dropping the `rooms`, `room_memberships`, and `messages` tables, since `ddl-auto=update` doesn't reliably alter existing column type definitions.
**Lesson:** `ddl-auto=update` is convenient for early development but has real limits — this is exactly the kind of scenario a proper migration tool (Flyway/Liquibase) is built to handle cleanly in production, where dropping tables isn't an option.
