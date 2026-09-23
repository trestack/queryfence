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
