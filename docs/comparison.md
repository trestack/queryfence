# How QueryFence compares

QueryFence **verifies** tenant isolation. Most tools below **enforce** it. They work together.

| | What it does | Where it works | What it does not cover |
|---|---|---|---|
| **Hibernate `@TenantId`** / filters | adds the tenant condition to entity queries at runtime | Hibernate entity queries | native SQL, `JdbcTemplate`, MyBatis, jOOQ; a disabled filter |
| **MyBatis-Plus `TenantLineInnerInterceptor`** | rewrites SQL at runtime to add the tenant condition | MyBatis-Plus | other data-access code; tables on the ignore list |
| **TenantLayer** | tenant isolation on Postgres RLS, plus scenario-based isolation tests (`@WithTenant`, `assertTenantCannotSee`, Testcontainers fixtures) | Postgres + Hibernate | other databases and ORMs; queries no scenario exercises |
| **Postgres RLS** | the database refuses rows of other tenants | Postgres | other databases; roles that bypass RLS; a wrong session tenant |
| **QueryFence** | checks every executed statement against a policy and fails the build | any JDBC `DataSource`, any database JSqlParser understands | runtime protection; only sees SQL your tests execute; does not check parameter values (v0.1) |

## Against scenario tests

TenantLayer and QueryFence both test isolation, from opposite directions:

- **Scenario tests prove behaviour.** "As tenant A, I cannot see tenant B's invoice." Each test
  proves one path end to end, against real policies and real data — and covers exactly the paths
  somebody thought to write.
- **QueryFence checks statements.** You write no isolation-specific tests; any existing test that
  touches a protected table is checked. It proves the filter is present, not that the behaviour is
  right.

Scenario tests catch wrong values and broken policies. Statement checks catch the query nobody
wrote a scenario for.

## Honest limits

- It only sees SQL your tests execute. Untested code paths are unchecked.
- It checks that a tenant predicate exists, not that the bound value is the *current* tenant.
- It is a test-time tool: it does not protect production if the tests are skipped.
