# Which JUnit does `queryfence-junit5` target?

**Status: proposed, waiting for the maintainer's decision.**

The JUnit version QueryFence compiles against is part of the contract with its users: it sets the
oldest JUnit they may run. This note collects the constraints and the measurements, and proposes
an answer; it does not decide.

## What users actually run

Almost nobody picks a JUnit version by hand; Spring Boot picks it for them.

| Spring Boot | JUnit Jupiter it manages |
|---|---|
| 3.3.x | 5.10.5 |
| 3.4.x | 5.11.4 |
| 3.5.x | 5.12.2 |
| 4.0.x, 4.1.x | 6.0.3 |

Latest releases at the time of writing: JUnit 5.13.4 and 6.1.3, Spring Boot 4.1.1 (Spring
Framework 7.0.9). JUnit 6 requires Java 17, which is already the QueryFence baseline, so the Java
version is not a constraint.

## What QueryFence actually uses

`queryfence-junit5` uses a small, stable part of the extension API:

- `BeforeTestExecutionCallback`, `AfterTestExecutionCallback`
- `ExtensionContext#getTestClass`, `#getTestMethod`, `#getDisplayName`
- `@RegisterExtension` on the user's side

It deliberately does **not** use `ExtensionContext.Store` or `Store.CloseableResource`: that type is
deprecated in 5.13 and removed in 6.0, and it is the usual reason an extension works on one major
version but not the other. The end-of-run report is flushed from a JVM shutdown hook instead, which
behaves the same everywhere.

`queryfence-spring-test` uses `ContextCustomizerFactory` and `TestExecutionListener`, registered
through `META-INF/spring.factories`.

`junit-jupiter-api` is a `provided` dependency, so the user's version is the one that runs.

## What we measured

The current code, compiled against Jupiter 5.13.4, passes its full test suite on:

| JUnit | Spring Boot | Result |
|---|---|---|
| 5.10.5 | 3.5.9 | pass |
| 5.11.4 | 3.5.9 | pass |
| 5.12.2 | 3.5.9 | pass |
| 5.13.4 | 3.5.9 | pass |
| 6.1.3 | 3.5.9 | pass |
| 6.1.3 | 4.1.1 (Spring Framework 7.0.9) | pass |

So today one artifact covers Spring Boot 3.3 through 4.1 without a shim, and `spring.factories`
registration still works in Spring Framework 7.

## Options

| Option | What it means | Cost |
|---|---|---|
| **A. Compile against 5.10.5** | The floor is Spring Boot 3.3 | We can never call an API added after 5.10 — but the compiler enforces the promise, which no policy document can |
| **B. Compile against 5.13.4** (today) | The floor is nominally 5.13 | Nothing stops us from calling a 5.11+ API and silently breaking Boot 3.3 users; only a CI matrix would catch it |
| **C. Compile against 6.x** | Only Spring Boot 4 users | Drops the majority of today's projects |
| **D. Two artifacts** (`-junit5`, `-junit6`) | Each compiled against its major | Two jars, two sets of docs, for an API we do not use differently |

## Proposal

**Option A plus a CI matrix.** Compile `queryfence-junit5` against Jupiter 5.10.5, keep the
dependency `provided`, and run the Phase 3 modules in CI against 5.10.5, 5.13.4 and 6.1.3 (the last
one together with Spring Boot 4). Compiling against the floor turns "do not use a newer API" from a
rule people have to remember into a compile error, and the matrix proves the promise on every push.

Option D becomes worth it only if the two majors ever need different code; today they do not.

## Open questions for the maintainer

1. How far back should 0.1 support: Spring Boot 3.3 (JUnit 5.10), or only 3.4+?
2. Do we advertise JUnit 6 / Spring Boot 4 support in the README for 0.1, given it is tested but
   not yet used in anger?
3. Spring Framework 7 keeps `spring.factories` for test infrastructure today. If that changes, the
   Spring module needs a second registration mechanism — worth a note in the release checklist.
