# QueryFence Design

## Goals

- Verify, in integration tests, that every SQL statement touching a protected table carries the required predicate.
- Work with any data-access technology that goes through a `javax.sql.DataSource`.
- Fail closed: anything we cannot prove safe is reported.

## Non-goals (v0.1)

- Enforcing isolation at runtime (use RLS, Hibernate filters or MyBatis interceptors for that).
- Checking parameter *values*.
- Spring Boot starter, UI, custom rule DSL.

## Rule: `require-predicate`

<!-- TODO: exact semantics — AND chain, alias binding, joins, subqueries, CTEs, UNION, UPDATE/DELETE, INSERT...SELECT -->

## Architecture

<!-- TODO -->

## Roadmap

| Version | Scope |
|---|---|
| 0.1 | require-predicate, update/delete-without-where, JUnit 5, JSON report |
| 0.2 | Baseline file, HTML report, parameter value check |
| 0.3 | Runtime mode, Spring Boot starter, text-to-SQL validator |
| 1.0 | API freeze |
