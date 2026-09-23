# How it works

```text
  your test
      │
      ▼
  code under test ── JPA / Hibernate / MyBatis / jOOQ / JdbcTemplate / raw JDBC
      │
      ▼  SQL
  ┌──────────────────────────────┐
  │ QueryFence DataSource proxy  │  records SQL + call site (StackWalker)
  └──────────────┬───────────────┘
                 │ SQL passes through unchanged
                 ▼
             database

  after each test:
  captured SQL ──► parse (JSqlParser) ──► check rules ──► FAIL / REPORT
                                                          console + report.json
```

## Capture

A `DataSource` proxy records every statement the driver executes, including each statement of a
JDBC batch and statements that failed while executing. The SQL is passed on unchanged: QueryFence
observes, it never rewrites or blocks.

A statement the driver rejects while *preparing* it never reaches the proxy, so it cannot be
checked — that test fails on its own anyway.

## Origin

`StackWalker` walks the stack at the moment of execution and takes the first frame that is not the
JDK, a driver, an ORM, a framework or QueryFence itself. That is the method that wrote the query —
`com.acme.order.OrderRepository#findByStatus (OrderRepository.java:42)` — not the framework that
executed it. Naming your own packages with `CaptureSettings.ofBasePackages("com.acme")` makes the
result exact.

## Check

Statements are parsed once (JSqlParser) and cached by SQL string, then each rule walks the parsed
statement: every occurrence of a protected table, in joins, subqueries, CTEs, each `UNION` branch,
`UPDATE`/`DELETE` targets and `INSERT ... SELECT` sources.

**Fail closed.** Anything QueryFence cannot prove safe is reported: SQL that does not parse, a
statement type it does not analyse yet, a predicate it cannot bind to a table.

## Report

Findings are grouped per policy, printed at the end of the run and written to
`target/queryfence/report.json`, each with the SQL, the rule, the fix and the origin. See
[CI](ci.md) for what to do with the file.

For the exact semantics — what counts as a predicate, how `AND`/`OR` compose, which `ON` clauses
fence which table — read the [design notes](DESIGN.md).
