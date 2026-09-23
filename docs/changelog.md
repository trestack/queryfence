# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `queryfence-core`: rule engine for `require-predicate`, `update-without-where` and
  `delete-without-where` as specified in `docs/DESIGN.md`, behind a small public API
  (`Policy`, `SqlChecker`, `Violation`). Every violation message explains how to fix it.
- Ignore unparseable statements that cannot read or write rows (`SET`, `SHOW`, `FLUSH`, DDL, ...)
  instead of reporting them, so test fixtures do not produce false positives.
- `queryfence-jdbc`: `QueryFence.wrap(DataSource, Policy)` records every statement executed through
  a data source (including JDBC batches), resolves the class, method, file and line that produced
  it, and reports the violations of the policy with that origin. Suppressions are matched by
  `Class#method`.
- `queryfence-junit5`: `QueryFenceExtension` loads `queryfence.yml`, fences a `DataSource`, fails
  the test in `FAIL` mode with the rule, the fix and the origin line, prints a summary at the end
  of the run and writes `target/queryfence/report.json`.
- `queryfence-spring-test`: every `DataSource` bean of a Spring test context is wrapped
  automatically; adding the dependency and a `queryfence.yml` is the whole setup.
- `queryfence-report` holds the run report; `queryfence-junit5` and `queryfence-spring-test` share
  it. The console summary and `report.json` are grouped per policy, each with its own mode.
- `@QueryFencePolicy("other.yml")` selects another policy file for a Spring test class, and
  `queryfence.enabled=false` switches the checks off for a run, with a loud warning.
- `queryfence-integration-tests`: Testcontainers matrix of {Hibernate, MyBatis, JdbcTemplate} ×
  {MySQL, Postgres} on framework-generated SQL (derived queries, JPQL, pagination, fetch joins,
  `EntityManager.find`, MyBatis dynamic SQL), each with a leak that must be caught and a correct
  query that must not be reported.
- `examples/`: two Spring Boot projects that run with `docker compose up` and ship a failing build.
