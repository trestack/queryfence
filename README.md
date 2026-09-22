<p align="center">
  <img src="docs/assets/queryfence.svg" width="120" alt="QueryFence logo">
</p>

<h1 align="center">QueryFence</h1>

<p align="center">
  <b>SQL policy testing for the JVM.</b><br>
  Catch tenant leaks, unbounded updates and unsafe queries in your integration tests.
</p>

<p align="center">
  <a href="https://github.com/trestack/queryfence/actions/workflows/ci.yml"><img src="https://github.com/trestack/queryfence/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License"></a>
</p>

> [!WARNING]
> **Work in progress.** QueryFence is in the design phase. Nothing is published to Maven Central
> yet, and every API shown below is a design sketch that may change before 0.1.0.

## The problem

In a multi-tenant application, every query on tenant data must filter by tenant:

```sql
SELECT * FROM purchase_order WHERE tenant_id = ? AND status = ?
```

Now someone adds one query and forgets the `tenant_id` part. What happens?

1. **Code review misses it.** The diff looks reasonable; the missing predicate is an absence, and absences are hard to see.
2. **Tests stay green.** Test fixtures usually contain a single tenant, so "all orders" and "this tenant's orders" return exactly the same rows.
3. **Production leaks.** Customer A opens a page and sees customer B's orders.

The bug is not in a framework you can audit once. It can come from JPQL, a native query, MyBatis XML,
jOOQ, a `JdbcTemplate` call, or code an AI assistant wrote five minutes ago.

QueryFence looks at the one thing all of those have in common: **the SQL that actually reaches the database.**
You declare the rule once; QueryFence checks every statement your integration tests execute and fails the
build when one breaks the rule, pointing at the class, method and line that produced it.

## Before and after

A repository method with a leak:

```java
public List<Order> findByStatus(long tenantId, String status) {
  return jdbc.query(
      "SELECT o.id, o.total FROM purchase_order o WHERE o.status = ?", // tenantId is never used
      ORDER_MAPPER,
      status);
}
```

The existing test passes, because the fixture has one tenant. With QueryFence enabled, the same test fails:

```text
QueryFence: 1 violation (mode FAIL)

  [tenant-isolation] require-predicate
    table   : purchase_order (alias o)
    problem : no predicate on o.tenant_id in the top-level AND chain of the WHERE clause
    sql     : SELECT o.id, o.total FROM purchase_order o WHERE o.status = ?
    origin  : com.acme.order.OrderRepository#findByStatus (OrderRepository.java:42)
    test    : com.acme.order.OrderServiceTest#listsPendingOrders

Full report: target/queryfence/report.json
```

The fix is the query you meant to write:

```java
"SELECT o.id, o.total FROM purchase_order o WHERE o.tenant_id = ? AND o.status = ?"
```

QueryFence is not fooled by predicates that look right but protect nothing:

```sql
-- under OR: returns every tenant's rows
... WHERE o.tenant_id = ? OR o.status = 'OPEN'

-- bound to the wrong table: order_item is unprotected
... FROM purchase_order o JOIN order_item i ON i.order_id = o.id WHERE o.tenant_id = ?
```

See [docs/DESIGN.md](docs/DESIGN.md) for the exact rule semantics and the known bypasses it blocks.

## Installation

> Not on Maven Central yet. Until 0.1.0 is released, build it locally with `./mvnw install`.

For a Spring Boot application:

```xml
<dependency>
  <groupId>io.github.trestack</groupId>
  <artifactId>queryfence-spring-test</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

Without Spring, use `queryfence-junit5` instead (see [Plain JDBC](#plain-jdbc)).
If you use several QueryFence modules, import `io.github.trestack:queryfence-bom` in
`<dependencyManagement>` to keep their versions aligned.

## Quickstart (Spring Boot)

Two steps: add the dependency above, and declare a policy. There is no annotation to add and no
test code to change.

### 1. Declare the policy

`src/test/resources/queryfence.yml`:

```yaml
version: 1
mode: FAIL               # FAIL the test, or REPORT only
onUnparseable: FAIL      # SQL we cannot parse is a violation unless you downgrade it

rules:
  - id: tenant-isolation
    type: require-predicate
    column: tenant_id
    tables: [purchase_order, order_item, invoice]

  - id: no-unbounded-update
    type: update-without-where

  - id: no-unbounded-delete
    type: delete-without-where

suppressions:
  - rule: tenant-isolation
    origin: com.acme.admin.PlatformReportJob#nightlyTotals
    reason: Cross-tenant report that runs as the platform operator, never as a tenant.
```

Suppressions are keyed by `Class#method` and **must** give a non-blank `reason`. A suppression
without a reason is a configuration error: QueryFence refuses to load the policy and every test
using it fails with a message pointing at the offending entry. Your production code never
depends on QueryFence: there are no annotations to add to it.

### 2. Run your tests

Your existing Spring tests stay exactly as they are:

```java
@SpringBootTest
class OrderServiceTest {

  @Autowired OrderService service;

  @Test
  void listsPendingOrders() {
    assertThat(service.pendingOrders(TENANT_A)).hasSize(2);
  }
}
```

`queryfence-spring-test` registers itself with the Spring TestContext Framework. In every test
context (`@SpringBootTest`, `@DataJpaTest`, `@JdbcTest`, ...) it wraps each `DataSource` bean,
loads `classpath:queryfence.yml`, and checks the SQL executed by each test method after it
finishes. In `FAIL` mode a violation fails that test; in `REPORT` mode it is only written to the
console and to `target/queryfence/report.json`.

### Plain JDBC

Without Spring, add `queryfence-junit5`, register the extension and wrap the `DataSource` your
code under test uses:

```java
class OrderRepositoryTest {

  @RegisterExtension
  static final QueryFenceExtension queryFence = QueryFenceExtension.fromClasspath("queryfence.yml");

  OrderRepository repository;

  @BeforeEach
  void setUp() {
    DataSource dataSource = queryFence.wrap(TestDatabase.dataSource());
    repository = new OrderRepository(new JdbcTemplate(dataSource));
  }

  @Test
  void findsPendingOrders() {
    assertThat(repository.findByStatus(TENANT_A, "PENDING")).hasSize(2);
  }
}
```

Every statement executed through the wrapped `DataSource` during a test is checked after that test.

Prefer code over YAML? The same policy can be built in Java:

```java
static final QueryFenceExtension queryFence =
    QueryFenceExtension.of(
        Policy.builder()
            .requirePredicate("tenant-isolation", "tenant_id", "purchase_order", "order_item", "invoice")
            .updateWithoutWhere("no-unbounded-update")
            .deleteWithoutWhere("no-unbounded-delete")
            .build());
```

## How it works

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

- **Capture:** a `DataSource` proxy (datasource-proxy) records each statement and the first stack frame
  outside JDBC, ORM and framework code, which is the method that produced it.
- **Parse:** statements are parsed once and cached by SQL string.
- **Check:** each rule walks the statement tree, including joins, subqueries, CTEs and every `UNION` branch.
- **Fail closed:** SQL that cannot be parsed, or that cannot be proven to satisfy a rule, is a violation.

QueryFence never modifies or blocks the SQL. It only observes it during tests.

## How it compares

QueryFence **verifies** tenant isolation. It does not **enforce** it at runtime. Most tools below
enforce it, and they work well together with QueryFence rather than competing with it.

| | What it does | Where it works | What it does not cover |
|---|---|---|---|
| **Hibernate `@TenantId`** / filters | Adds the tenant condition to entity queries at runtime | Hibernate entity queries | Native SQL, `JdbcTemplate`, MyBatis, jOOQ; a disabled filter |
| **MyBatis-Plus `TenantLineInnerInterceptor`** | Rewrites SQL at runtime to add the tenant condition | MyBatis-Plus | Other data-access code; tables on the ignore list |
| **TenantLayer** | Tenant isolation built on Postgres Row-Level Security, plus scenario-based isolation tests (`@WithTenant`, `assertTenantCannotSee`, Testcontainers fixtures) | Postgres + Hibernate | Other databases and ORMs; queries no scenario exercises |
| **Postgres RLS** | The database refuses rows from other tenants | Postgres | Other databases; roles that bypass RLS; wrong session tenant |
| **QueryFence** | Checks every executed statement against a policy and fails the build | Any JDBC `DataSource`, any database JSqlParser understands | Runtime protection; only sees SQL your tests execute; does not check parameter values (v0.1) |

Why use both? Runtime enforcement is only as good as its coverage: a native query or a new data-access
path can quietly fall outside it. QueryFence shows, in CI, whether every query your tests run is actually
fenced, whichever layer does the fencing. If you rely only on RLS, a `REPORT`-mode run also tells you
which queries depend on it.

TenantLayer and QueryFence both test isolation, but from opposite directions:

- **TenantLayer tests scenarios you write.** "As tenant A, I cannot see tenant B's invoice." Each test
  proves one behaviour end to end, against real data and real RLS policies. It covers exactly the
  paths someone thought to write a scenario for.
- **QueryFence checks every statement your tests happen to run.** You write no isolation-specific tests;
  any existing test that touches `purchase_order` is checked. It proves the tenant predicate is present,
  not that the behaviour is correct end to end.

Scenario tests catch wrong values and broken policies; statement checks catch the query nobody wrote a
scenario for. The two complement each other.

Honest limits of QueryFence:

- It only sees SQL that your tests execute. Untested code paths are unchecked.
- It checks that a tenant predicate exists, not that the bound value is the *current* tenant.
- It is a test-time tool. It does not protect production if tests are skipped.

## Status and roadmap

Phase 0: design. Next come the core rule engine and a golden corpus of SQL cases, then JDBC capture,
the JUnit 5 extension, and a Testcontainers matrix on MySQL and Postgres.
See [docs/DESIGN.md](docs/DESIGN.md#roadmap).

Found a SQL pattern that slips past a rule? That is a security issue: please report it privately,
see [SECURITY.md](SECURITY.md).

## License

[Apache License 2.0](LICENSE). Part of [Trestack](https://github.com/trestack): backend tools that bend, not break.
