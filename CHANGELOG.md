# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `queryfence-core`: rule engine for `require-predicate`, `update-without-where` and
  `delete-without-where` as specified in `docs/DESIGN.md`, behind a small public API
  (`Policy`, `SqlChecker`, `Violation`). Every violation message explains how to fix it.
