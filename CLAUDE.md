# CLAUDE.md — QueryFence

Context for AI coding agents working in this repository. Read fully before making changes.

## What this project is

**QueryFence** — SQL policy testing for the JVM ("ArchUnit for SQL").

Users declare rules once (e.g. every query touching `purchase_order` must filter by `tenant_id`).
During integration tests, QueryFence captures every SQL statement actually sent to the database
(from JPA/Hibernate, MyBatis, jOOQ, JdbcTemplate, native queries, AI-written code), parses it,
checks it against the policy, and fails the build on violations — pointing to the exact
class, method and line that produced the SQL.

Primary problem: multi-tenant data leaks caused by a missing `WHERE tenant_id = ?`.

It **verifies** isolation. It does **not** replace runtime enforcement (Postgres RLS,
Hibernate `@TenantId`/filters, MyBatis-Plus tenant interceptor). It complements them.

## Brand and coordinates

- Umbrella brand: **Trestack** ("tre" = bamboo; tagline: *Backend tools that bend, not break.*)
- GitHub: `github.com/trestack/queryfence`
- Maven groupId: `io.github.trestack` (no domain yet; may relocate to `dev.trestack` later)
- Java packages: `dev.trestack.queryfence.*` — keep these even though groupId differs,
  so a future groupId relocation never breaks user imports
- License: Apache-2.0
- Palette: Bamboo `#1F4D34`, Shoot `#B8C97A`, Ink `#15201A`, Paper `#F4F1E8`

## Modules

| Module | Purpose | Allowed dependencies |
|---|---|---|
| `queryfence-core` | Parser adapter, rule engine, policy model | **JSqlParser only** |
| `queryfence-jdbc` | Capture SQL via datasource-proxy, resolve origin via `StackWalker` | core, datasource-proxy |
| `queryfence-junit5` | JUnit 5 extension, YAML policy loading, console + JSON report | jdbc, junit-jupiter-api, snakeyaml |
| `queryfence-bom` | Version alignment | — |

Planned later: `queryfence-spring-test` (auto-wrap DataSource bean in Spring test context),
`queryfence-integration-tests` (Testcontainers matrix), `examples/`.

## Architecture rules (do not break)

1. `queryfence-core` must never depend on JDBC, JUnit, Spring or YAML libraries.
2. **Fail closed.** Anything we cannot prove safe is a violation. Unparseable SQL is a
   violation by default (`onUnparseable: FAIL`), user may downgrade.
3. Suppressions live in the policy (YAML / builder) keyed by `Class#method` and **require a
   reason**. Never add annotations that production code would need to depend on.
4. Public API is small and explicit. Anything not meant for users goes in an `internal`
   package. Prefer interfaces + static factories over exposing implementation classes.
5. Parse results are cached by SQL string.

## Rule semantics: `require-predicate`

A statement passes when, for **every occurrence** of a protected table — in FROM, JOIN,
subqueries (WHERE/FROM/SELECT list), CTEs, each UNION branch, UPDATE/DELETE targets
(including joined forms), and INSERT...SELECT sources — the condition at that scope contains
a predicate that:

- has the form `<alias>.<column> = <parameter or literal>` or `<alias>.<column> IN (...)`
- sits in the **top-level AND chain** (never under OR — `tenant_id = ? OR 1=1` is a bypass)
- is bound to **that table's alias** (a predicate on `o.tenant_id` does not protect `i`)
- may appear in the JOIN's ON clause or the WHERE clause

INSERT: the column must be present in the column list.
Identifiers compare case-insensitively; handle quoted identifiers (MySQL backticks,
Postgres double quotes) and schema-qualified names (`app.purchase_order`).

Checking parameter *values* against the current tenant is out of scope for v0.1.

## v0.1 scope

In: `require-predicate`, `update-without-where`, `delete-without-where`, JUnit 5 extension,
modes FAIL and REPORT, YAML + Java builder policy, suppressions with reason, console report,
`target/queryfence/report.json`, examples on MySQL and Postgres.

Out (do not build yet): runtime blocking, Spring Boot starter, baseline file, HTML report,
parameter value checks, custom rule DSL, UI.

## Testing approach

- **Golden corpus** is the heart of the project: data files of SQL + expected violations,
  loaded by a parameterized test. Target ≥150 cases for v0.1, grouped by topic
  (basic, alias, join, subquery, CTE, union, OR-bypass, nesting, case, quoting,
  schema-qualified, dialects, UPDATE/DELETE, INSERT).
- Every rule change or bug fix adds corpus cases first (test-first).
- PIT mutation testing on the rule engine; target >85% mutation score.
- Integration tests via Testcontainers: {Hibernate, MyBatis, JdbcTemplate} × {MySQL, Postgres},
  each with one deliberately leaky query that must be caught.

## Conventions

- Java 17 baseline; CI runs 17, 21, 25.
- Format: `./mvnw spotless:apply` (google-java-format, license header required).
- Before every commit: `./mvnw verify` must pass.
- Conventional commits: `feat:`, `fix:`, `test:`, `docs:`, `chore:`, `ci:`.
- Update `CHANGELOG.md` under `[Unreleased]` for user-visible changes.
- Keep versions in the parent `pom.xml` `<properties>`.

## Security — never do these

- Never read, print, create or commit GPG keys, passphrases, Central Portal tokens or any
  secret. Release credentials live only in GitHub Secrets.
- Never run `-Prelease deploy`, push tags, or trigger a release. Releasing is done by the
  maintainer manually.
- Rule-bypass reports are security issues: see `SECURITY.md`.

## Current status and plan

Phase 0 (current): write README, complete `docs/DESIGN.md`, draft first 30 golden cases.
No production code yet beyond the scaffold.

1. Phase 0 — design on paper (README, DESIGN.md, 30 cases)
2. Phase 1 — core + golden corpus (≥150 cases) + PIT
3. Phase 2 — jdbc capture + origin detection
4. Phase 3 — JUnit 5 extension + Spring test support + reports
5. Phase 4 — Testcontainers matrix + examples
6. Phase 5 — dogfood in real projects in REPORT mode (target <5% false positives)
7. Phase 6 — docs site (MkDocs Material, `trestack.github.io/queryfence`, include `llms.txt`)
8. Phase 7 — release 0.1.0 (tag `v0.1.0`; workflow uploads with autoPublish=false)
9. Phase 8 — launch post and community

Competitive landscape to stay aware of: TenantLayer (Postgres RLS, Hibernate only),
MyBatis-Plus `TenantLineInnerInterceptor`, Hibernate `@TenantId`, Python `sql-guardrail`.
