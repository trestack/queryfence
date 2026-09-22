# QueryFence Design

Status: **draft for v0.1**. This document is the specification the rule engine and the golden
corpus are written against. Numbered clauses (`RP-3`, `UW-1`, ...) are referenced from corpus cases.

## Goals

- Verify, in integration tests, that every SQL statement touching a protected table carries the
  required predicate.
- Work with any data-access technology that goes through a `javax.sql.DataSource`.
- Point at the exact class, method and line that produced an offending statement.
- **Fail closed:** anything we cannot prove safe is reported.

## Non-goals (v0.1)

- **Runtime enforcement.** QueryFence never rewrites or blocks SQL. Use Postgres RLS, Hibernate
  `@TenantId`/filters or MyBatis-Plus `TenantLineInnerInterceptor` for that.
- **Parameter value checks.** We prove that `tenant_id = ?` exists, not that `?` is bound to the
  current tenant.
- **SQL that tests never execute.** No static analysis of source code, mapper XML or JPQL.
- **Seeing inside the database.** Views, stored procedures, triggers and `CALL` are opaque.
- **DDL and `TRUNCATE`.** Schema statements are ignored by all rules.
- **Upsert conflict branches.** `ON CONFLICT DO UPDATE` / `ON DUPLICATE KEY UPDATE` are not analysed
  beyond the `INSERT` column list.
- **Parallel test execution.** Statements are attributed to the running test by time window;
  JUnit parallel execution is unsupported in v0.1.
- Spring Boot starter for production, baseline file, HTML report, custom rule DSL, UI.

## Terminology

- **Protected table** — a table listed in a `require-predicate` rule's `tables`.
- **Occurrence** — one place a protected table appears as a row source: a `FROM` item, a `JOIN`
  item, an `UPDATE`/`DELETE` target, an `INSERT` target, or a joined table of a multi-table
  `UPDATE`/`DELETE` (`JOIN`, `FROM`, `USING`). A self-join has two occurrences.
- **Reference name** — the alias of an occurrence if it has one, otherwise its unqualified table name.
- **Query block** — one `SELECT ... FROM ... WHERE ...` unit. Each subquery, each CTE body and each
  branch of a set operation (`UNION`, `INTERSECT`, `EXCEPT`) is its own block. `UPDATE` and `DELETE`
  statements are blocks too.
- **Top-level AND chain** — the list of conjuncts obtained by splitting a condition on `AND`,
  recursively, through redundant parentheses only. Anything under `OR`, `NOT`, `CASE`, a function
  call or a comparison is a single opaque conjunct.

  ```sql
  a AND (b AND c)         -- chain: a, b, c
  a AND (b OR c)          -- chain: a, (b OR c)
  NOT (a AND b)           -- chain: NOT (a AND b)
  ```

## Rule: `require-predicate`

```yaml
- id: tenant-isolation
  type: require-predicate
  column: tenant_id
  tables: [purchase_order, order_item, invoice]
```

A statement passes when **every occurrence** of every protected table is *fenced*. Each unfenced
occurrence is one violation.

### RP-1 Every occurrence is checked independently

Each occurrence must be fenced by a predicate bound to its own reference name, in its own query
block. Protecting one occurrence never protects another.

```sql
-- pass
SELECT * FROM purchase_order o WHERE o.tenant_id = ?

-- violation: purchase_order (o) — the only filter is on status
SELECT * FROM purchase_order o WHERE o.status = ?

-- violation: purchase_order (p) — self-join, only o is fenced
SELECT * FROM purchase_order o JOIN purchase_order p ON p.parent_id = o.id WHERE o.tenant_id = ?
```

### RP-2 Predicate form

A conjunct fences an occurrence when it has one of these forms, where `ref` is the occurrence's
reference name and `col` is the rule's `column`:

| Form | Example |
|---|---|
| `ref.col = value` or `value = ref.col` | `o.tenant_id = ?`, `42 = o.tenant_id` |
| `ref.col IN (value, ...)` with at least one value | `o.tenant_id IN (?, ?)` |

`value` is a JDBC parameter (`?`), a named or positional parameter (`:tenant`, `$1`), a string or
numeric literal, or a `CAST` of one of those (`CAST(? AS BIGINT)`, `?::bigint`).

Everything else does **not** fence, including: `<>`, `!=`, `<`, `>`, `LIKE`, `BETWEEN`,
`IS NULL`, `IS NOT NULL`, `IN (subquery)`, comparison with a function call
(`tenant_id = current_setting('app.tenant')`), and comparison with another column
(`i.tenant_id = o.tenant_id`).

```sql
-- pass
SELECT * FROM invoice WHERE invoice.tenant_id IN (?, ?)

-- violation: comparison with a column, not a value
SELECT * FROM purchase_order o JOIN order_item i ON i.order_id = o.id AND i.tenant_id = o.tenant_id
WHERE o.tenant_id = ?

-- violation: inequality selects every other tenant
SELECT * FROM purchase_order o WHERE o.tenant_id <> ?
```

### RP-3 Top-level AND chain only

The fencing conjunct must be in the top-level AND chain of a permitted condition (RP-5). A
predicate under `OR` is never accepted, even when every branch looks restrictive — use `IN`.

```sql
-- pass
SELECT * FROM purchase_order o WHERE (o.status = ? AND (o.tenant_id = ?))

-- violation: OR makes the tenant filter optional
SELECT * FROM purchase_order o WHERE o.tenant_id = ? OR o.status = 'OPEN'

-- violation: rejected although both branches restrict; rewrite as IN (?, ?)
SELECT * FROM purchase_order o WHERE o.tenant_id = ? OR o.tenant_id = ?
```

### RP-4 Alias binding

The column must be qualified with the occurrence's reference name. An unqualified column fences an
occurrence **only** when that occurrence is the sole row source of its query block. When a table has
an alias, the table name is not a valid qualifier.

```sql
-- pass: single row source, unqualified column is unambiguous
SELECT * FROM purchase_order WHERE tenant_id = ?

-- pass: no alias, table name as qualifier
SELECT * FROM purchase_order WHERE purchase_order.tenant_id = ?

-- violation: order_item (i) — the predicate is bound to o
SELECT * FROM purchase_order o JOIN order_item i ON i.order_id = o.id WHERE o.tenant_id = ?

-- violation: purchase_order (o) and order_item (i) — unqualified column in a two-table block
SELECT * FROM purchase_order o JOIN order_item i ON i.order_id = o.id WHERE tenant_id = ?
```

### RP-5 Where the predicate may appear

For an occurrence `T` in a query block, a fencing conjunct is accepted from:

1. the top-level AND chain of the block's `WHERE` clause;
2. the top-level AND chain of the `ON` clause of any `INNER` (or plain) `JOIN` in the block, since
   inner-join conditions filter exactly like `WHERE`;
3. the `ON` clause of the `LEFT JOIN` that introduces `T` (T is the nullable side, so the condition
   limits which of its rows can appear).

Not accepted: the `ON` clause of a `LEFT JOIN` for the preserved (left) side, the `ON` clause of a
`RIGHT JOIN` for the table it introduces, and any `FULL JOIN` `ON` clause. Those conditions do not
remove rows of the preserved side. `HAVING` is not accepted.

```sql
-- pass: tenant filter for o in an inner join's ON clause
SELECT * FROM purchase_order o JOIN order_item i ON i.order_id = o.id AND o.tenant_id = ? AND i.tenant_id = ?

-- pass: i is the nullable side of its own LEFT JOIN
SELECT * FROM purchase_order o LEFT JOIN order_item i ON i.order_id = o.id AND i.tenant_id = ?
WHERE o.tenant_id = ?

-- violation: purchase_order (o) — a LEFT JOIN condition does not filter the left side
SELECT * FROM purchase_order o LEFT JOIN order_item i ON i.order_id = o.id AND o.tenant_id = ? AND i.tenant_id = ?
```

### RP-6 Subqueries

Every subquery is its own query block and is checked on its own, wherever it appears: `WHERE`
(`IN`, `EXISTS`, scalar comparison), `SELECT` list, `FROM` (derived table), `HAVING`, `SET` of an
`UPDATE`. A predicate in an outer block never fences an occurrence in an inner block, and a filter
applied to a derived table from outside does not fence the table inside it.

```sql
-- pass
SELECT * FROM purchase_order o WHERE o.tenant_id = ?
  AND EXISTS (SELECT 1 FROM order_item i WHERE i.order_id = o.id AND i.tenant_id = ?)

-- violation: order_item (i) inside EXISTS is unfenced
SELECT * FROM purchase_order o WHERE o.tenant_id = ?
  AND EXISTS (SELECT 1 FROM order_item i WHERE i.order_id = o.id)

-- violation: purchase_order inside the derived table; the outer filter does not count
SELECT * FROM (SELECT * FROM purchase_order) t WHERE t.tenant_id = ?
```

### RP-7 Set operations

Each branch of `UNION`, `UNION ALL`, `INTERSECT` and `EXCEPT` is its own query block.

```sql
-- pass
SELECT id FROM purchase_order WHERE tenant_id = ? UNION SELECT id FROM invoice WHERE tenant_id = ?

-- violation: invoice in the second branch
SELECT id FROM purchase_order WHERE tenant_id = ? UNION SELECT id FROM invoice
```

### RP-8 Common table expressions

Each CTE body is a query block. A reference to a CTE name is not an occurrence, even when the CTE
has the same name as a protected table. As with derived tables, a filter applied where the CTE is
used does not fence the tables inside its body. Recursive CTEs: each branch is checked.

```sql
-- pass
WITH mine AS (SELECT * FROM purchase_order WHERE tenant_id = ?) SELECT * FROM mine WHERE mine.status = ?

-- violation: purchase_order in the CTE body
WITH all_orders AS (SELECT * FROM purchase_order) SELECT * FROM all_orders a WHERE a.tenant_id = ?
```

### RP-9 UPDATE and DELETE

The target is an occurrence; its fencing predicate must be in the statement's `WHERE` chain (or an
inner-join `ON` chain for MySQL multi-table forms). Joined tables of multi-table forms are
occurrences under the same rules. Subqueries in `SET` and `WHERE` are checked per RP-6.

```sql
-- pass
UPDATE purchase_order SET status = ? WHERE tenant_id = ? AND id = ?

-- violation: purchase_order — only filtered by id
DELETE FROM purchase_order WHERE id = ?

-- violation: order_item (i) in a MySQL multi-table UPDATE
UPDATE purchase_order o JOIN order_item i ON i.order_id = o.id SET i.price = ? WHERE o.tenant_id = ?
```

### RP-10 INSERT

`INSERT INTO t (...)` into a protected table passes only when the rule's column appears in the
explicit column list (MySQL `INSERT ... SET col = ...` counts). An `INSERT` without a column list
is a violation: we cannot prove which columns are set. The inserted value is not checked (v0.1).
For `INSERT ... SELECT`, the `SELECT` is checked as a query block per RP-1..RP-8.
`REPLACE INTO` (MySQL) is treated like `INSERT`.

```sql
-- pass
INSERT INTO purchase_order (id, tenant_id, status) VALUES (?, ?, ?)

-- violation: column list has no tenant_id
INSERT INTO purchase_order (id, status) VALUES (?, ?)

-- violation: no column list
INSERT INTO purchase_order VALUES (?, ?, ?)

-- violation: the source purchase_order (o) of INSERT ... SELECT
INSERT INTO invoice (tenant_id, order_id) SELECT o.tenant_id, o.id FROM purchase_order o
```

### RP-11 Identifiers

- Identifiers compare case-insensitively after removing quotes (MySQL backticks, ANSI/Postgres
  double quotes, SQL Server brackets).
- A `tables` entry without a schema matches the table in any schema (`app.purchase_order`
  matches `purchase_order`). An entry with a schema (`app.purchase_order`) matches references with
  that schema **and** unqualified references, since the search path is unknown.

### RP-12 Statements out of scope of the rule

Statements that touch no protected table pass. DDL, `TRUNCATE`, transaction control (`BEGIN`,
`COMMIT`, `SET`, `SAVEPOINT`), `SHOW`/`EXPLAIN` and `CALL` are ignored. A DML statement type the
rule does not analyse (`MERGE` in v0.1) that references a protected table is a violation
(`UNSUPPORTED_STATEMENT`). A string holding several statements separated by `;` is split and each
statement is checked.

## Rule: `update-without-where`

```yaml
- id: no-unbounded-update
  type: update-without-where
```

Applies to every `UPDATE`, on any table.

- **UW-1** An `UPDATE` without a `WHERE` clause is a violation, including multi-table forms whose
  only restriction is a join, and statements with `LIMIT` but no `WHERE`.
- **UW-2** An `UPDATE` whose `WHERE` is a tautology is a violation. A tautology is a condition made
  only of constants that is true (`1 = 1`, `TRUE`, `1`, `'a' = 'a'`), or a top-level OR chain with
  at least one such operand (`id = ? OR 1 = 1`).
- **UW-3** Anything else passes. `WHERE 1 = 1 AND id = ?` passes: the constant is a harmless
  conjunct, a common artifact of query builders.

```sql
-- pass
UPDATE purchase_order SET status = ? WHERE id = ?
-- violation (UW-1)
UPDATE purchase_order SET status = 'CLOSED'
-- violation (UW-2)
UPDATE purchase_order SET status = ? WHERE 1 = 1
```

## Rule: `delete-without-where`

Same definition as `update-without-where`, for `DELETE` (clauses **DW-1..DW-3**). Postgres
`DELETE ... USING` and MySQL multi-table `DELETE` without `WHERE` violate DW-1. `TRUNCATE` is DDL and
out of scope.

```sql
-- pass
DELETE FROM order_item WHERE order_id = ?
-- violation (DW-1)
DELETE FROM order_item
-- violation (DW-2)
DELETE FROM order_item WHERE order_id = ? OR TRUE
```

## Unparseable SQL

A statement JSqlParser cannot parse produces an `UNPARSEABLE` violation, whatever tables it may
touch. `onUnparseable: FAIL` (default) fails the test; `onUnparseable: REPORT` only reports it.

## Known bypasses and how they are blocked

Every entry below has corpus cases. New bypasses are security issues (see `SECURITY.md`) and are
fixed test-first by adding corpus cases.

| Bypass | Example | Blocked by |
|---|---|---|
| Predicate under `OR` | `WHERE o.tenant_id = ? OR 1 = 1` | RP-3 |
| Predicate under `NOT` / negation | `WHERE NOT (o.tenant_id <> ?)` | RP-2, RP-3 (opaque conjunct) |
| Inequality or range | `WHERE o.tenant_id <> ?`, `> 0` | RP-2 |
| Alias bound to the wrong table | `... JOIN order_item i ... WHERE o.tenant_id = ?` | RP-1, RP-4 |
| Join without a condition for the joined table | `JOIN invoice v ON v.order_id = o.id` | RP-1 |
| Self-join with one side fenced | `purchase_order o JOIN purchase_order p` | RP-1 |
| Tenant filter on the preserved side of a `LEFT JOIN` placed in `ON` | `LEFT JOIN ... ON ... AND o.tenant_id = ?` | RP-5 |
| Unfenced subquery (`EXISTS`, `IN`, scalar, `SELECT` list) | `EXISTS (SELECT 1 FROM order_item i WHERE ...)` | RP-6 |
| Filter outside a derived table | `FROM (SELECT * FROM purchase_order) t WHERE t.tenant_id = ?` | RP-6 |
| Unfenced `UNION` branch | `... UNION SELECT id FROM invoice` | RP-7 |
| Unfenced CTE body | `WITH a AS (SELECT * FROM purchase_order) ...` | RP-8 |
| Column-to-column comparison | `i.tenant_id = o.tenant_id` alone | RP-2 |
| Predicate hidden in a comment | `WHERE o.status = ? -- AND o.tenant_id = ?` | parser drops comments; RP-1 |
| `INSERT` without column list | `INSERT INTO purchase_order VALUES (...)` | RP-10 |
| Unfenced `INSERT ... SELECT` source | `INSERT INTO t (...) SELECT ... FROM purchase_order` | RP-10 |
| Quoting / case / schema tricks | `` `Purchase_Order` ``, `app."PURCHASE_ORDER"` | RP-11 |
| SQL the parser does not understand | vendor syntax | fail closed: `UNPARSEABLE` |
| Unsupported DML | `MERGE INTO purchase_order ...` | RP-12 |

Known **unblocked** paths in v0.1 (documented limitations): views and stored procedures over
protected tables (list views in `tables` as a workaround), the value bound to `?`, upsert
conflict branches, and SQL that no test executes.

## Violation model

Each violation carries:

| Field | Example |
|---|---|
| `ruleId` | `tenant-isolation` |
| `ruleType` | `require-predicate` |
| `code` | `MISSING_PREDICATE`, `MISSING_INSERT_COLUMN`, `NO_WHERE`, `TAUTOLOGICAL_WHERE`, `UNSUPPORTED_STATEMENT`, `UNPARSEABLE` |
| `table`, `alias` | `order_item`, `i` (for require-predicate) |
| `sql` | the statement as sent to the driver |
| `origin` | `com.acme.order.OrderRepository#findByStatus (OrderRepository.java:42)` |
| `test` | `com.acme.order.OrderServiceTest#listsPendingOrders` |

## Architecture

### Modules

| Module | Responsibility | Depends on |
|---|---|---|
| `queryfence-core` | Policy model, parser adapter, rule engine, violation model | JSqlParser only |
| `queryfence-jdbc` | `DataSource` proxy, statement capture, origin resolution | core, datasource-proxy |
| `queryfence-junit5` | JUnit 5 extension, YAML policy loading, console and JSON reports | jdbc, junit-jupiter-api, snakeyaml |
| `queryfence-spring-test` | Wrap every `DataSource` bean in Spring test contexts; zero-code setup | junit5, spring-test and spring-context (`provided`) |
| `queryfence-bom` | Version alignment | — |

`queryfence-core` never depends on JDBC, JUnit, Spring or YAML, so the engine can be reused later
(runtime mode, text-to-SQL validation) without dragging test libraries along.

### Processing flow

```text
 test starts ──► Recorder.open(testId)
                     │
 code under test ──► wrapped DataSource ──► CapturedStatement(sql, origin, thread)
                     │                         origin = first stack frame outside
                     │                         JDBC / ORM / framework / QueryFence
 test ends ────► Recorder.close() ──► statements
                                          │
                     ParsedStatement cache (key: SQL string)
                                          │
                     rules ──► violations ──► suppressions (Class#method + reason)
                                          │
                     FAIL: throw AssertionError listing violations
                     REPORT: log only
                                          │
 JVM / launcher session ends ──► target/queryfence/report.json
```

- **Capture window.** Only statements executed during the test method body are checked
  (JUnit `BeforeTestExecutionCallback` to `AfterTestExecutionCallback`). Fixture code in
  `@BeforeEach`/`@AfterEach`, context startup and migrations are not checked in v0.1.
- **Origin resolution.** `StackWalker` returns the first frame whose class is not in an ignored
  package: `java.`, `javax.`, `jakarta.`, `jdk.`, `sun.`, `org.hibernate.`, `org.springframework.`,
  `org.apache.ibatis.`, `org.mybatis.`, `org.jooq.`, `com.zaxxer.`, `net.ttddyy.`,
  `dev.trestack.queryfence.`, plus generated proxy classes (`$$`, `$Proxy`). Users can add prefixes.
- **Suppressions** match the resolved origin exactly on class name and method name (no overloads).
  A suppression without a non-blank `reason` is a configuration error at load time. Suppressions
  that matched nothing during the run are listed in the report as stale.
- **Parse cache** is keyed by the exact SQL string and shared across tests.

### Public API (sketch)

Everything else lives in `internal` packages.

```java
// queryfence-core — dev.trestack.queryfence.core
Policy policy = Policy.builder()
    .requirePredicate("tenant-isolation", "tenant_id", "purchase_order", "order_item")
    .updateWithoutWhere("no-unbounded-update")
    .deleteWithoutWhere("no-unbounded-delete")
    .suppress("tenant-isolation", "com.acme.admin.PlatformReportJob#nightlyTotals", "reason ...")
    .onUnparseable(Severity.FAIL)
    .build();

SqlChecker checker = SqlChecker.of(policy);
List<Violation> violations = checker.check("SELECT ...");   // no origin, no suppression

// queryfence-junit5 — dev.trestack.queryfence.junit5
@RegisterExtension
static final QueryFenceExtension queryFence = QueryFenceExtension.fromClasspath("queryfence.yml");
// or QueryFenceExtension.of(policy)
DataSource wrapped = queryFence.wrap(dataSource);

// queryfence-spring-test — no user-facing API; registered through META-INF/spring.factories
// (ContextCustomizerFactory + TestExecutionListener), reads classpath:queryfence.yml.
```

### Policy file

```yaml
version: 1                       # required; only 1 is valid
mode: FAIL                       # FAIL | REPORT, default FAIL
onUnparseable: FAIL              # FAIL | REPORT, default FAIL
rules:
  - id: tenant-isolation         # required, unique
    type: require-predicate      # require-predicate | update-without-where | delete-without-where
    column: tenant_id            # require-predicate only, required
    tables: [purchase_order]     # require-predicate only, required, non-empty
suppressions:
  - rule: tenant-isolation       # must name an existing rule id
    origin: com.acme.Foo#bar     # required, Class#method
    reason: why this is safe     # required, non-blank
```

Unknown keys, duplicate rule ids and missing required fields are configuration errors.

## Roadmap

| Version | Scope |
|---|---|
| 0.1 | require-predicate, update/delete-without-where, JUnit 5, Spring test support, JSON report |
| 0.2 | Baseline file, HTML report, parameter value check |
| 0.3 | Runtime mode, Spring Boot starter, text-to-SQL validator |
| 1.0 | API freeze |
