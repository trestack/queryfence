# QueryFence

> SQL policy testing for the JVM. Catch tenant leaks, unbounded updates and unsafe queries in your integration tests.

[![CI](https://github.com/trestack/queryfence/actions/workflows/ci.yml/badge.svg)](https://github.com/trestack/queryfence/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

> ⚠️ **Work in progress.** Not yet published to Maven Central. APIs will change before 1.0.

## The problem

One query forgets `WHERE tenant_id = ?`. Code review misses it, tests stay green because test data has a single tenant, and in production one customer sees another customer's data.

<!-- TODO: before/after example -->

## How it works

<!-- TODO: diagram -->

## Quickstart

<!-- TODO -->

## Status

See the [roadmap](docs/DESIGN.md#roadmap).

## License

[Apache License 2.0](LICENSE)
