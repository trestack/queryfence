# Phase 5 — QueryFence against an application it did not design

Measured on `dogfood/` (Northwind Shop) on 2026-09-24 with QueryFence 0.1.0-SNAPSHOT, Postgres 17,
Spring Boot 3.5.9, Hibernate 6.6, MyBatis 3.0.5. The adoption path of
[docs/ADOPTION.md](../docs/ADOPTION.md) was followed literally, from the outside: add the
dependency, get the table list out of `information_schema`, write the policy, run the tests that
already existed, read the report with `tools/queryfence-summary.py`, triage.

## The numbers

```
queryfence.yml [REPORT]: 20 findings, 75 statements, 62 tests

By code                        By table                   Verdict
MISSING_PREDICATE       17     purchase_order    6        real leak            15
MISSING_INSERT_COLUMN    1     customer          5        false positive        4
PRIMARY_KEY_LOOKUP       1     invoice           3        deliberate exception  1
UNPARSEABLE              1     order_item        3
                               audit_log         1
                               payment           1
                               (not a table)     1
```

- 75 statements checked, 20 findings, **20 distinct origins**: every finding is its own fix.
- **False-positive rate: 4 of 20 findings = 20%**, or 4 of 75 statements = 5.3%.
- Of the 4, three are limitations already documented in `DESIGN.md` (two ORM association loads, one
  derived table) and one is a parser bug (below). Nothing was reported for a reason we had not
  already written down.

Every query the application writes correctly passed, and that is the more interesting half of the
result. No finding for: derived queries with the tenant, pagination and its `count(*)` query, JPQL
and native queries with the tenant, transitive joins (`i.tenant_id = o.tenant_id` anchored on the
other side, including a `LEFT JOIN`), correlated `exists` / `not exists` subqueries, `group by ...
having`, a CTE with the filter inside it, `in (?, ?) and tenant_id = ?`, MyBatis dynamic SQL with
`<if>` and `<foreach>`, the unprotected `product` and `currency` tables, `select nextval(...)`, an
`INSERT` that sets `tenant_id`, and a derived `delete ... where tenant_id = ? and order_id = ?`.

## Triage

### Real leaks — 15

| Origin | Code | What is wrong |
|---|---|---|
| `AuditService#recordFast` | `MISSING_INSERT_COLUMN` | native `insert into audit_log` copied from an older column list |
| `OrderService#itemsOf(long)` | `MISSING_PREDICATE` | `order_item` by `order_id` only; the call path never checks the tenant |
| `OrderService#freeSearch` | `MISSING_PREDICATE` | `Specification` search built without `ofTenant(...)` |
| `OrderService#ofActiveCustomersSorted` | `MISSING_PREDICATE` | JPQL entity join to `customer` without its tenant condition |
| `OrderService#migrateCurrency` | `MISSING_PREDICATE` | bulk `update purchase_order` over every tenant |
| `OrderService#overdue` | `MISSING_PREDICATE` | native join to `invoice` without its tenant condition |
| `OrderService#everythingWithStatus` | `MISSING_PREDICATE` | `findByStatus` from the first prototype |
| `OrderSearchService#exportRows` | `MISSING_PREDICATE` | MyBatis join to `customer` on `id` only |
| `OrderSearchService#searchAnyTenant` | `MISSING_PREDICATE` | dynamic `<where>` with a null tenant: the filter disappears |
| `CustomerService#archiveClosed` | `MISSING_PREDICATE` | bulk `update customer` over every tenant |
| `CustomerService#byId` | `PRIMARY_KEY_LOOKUP` | `findById` from the support console |
| `CustomerService#closedEverywhere` | `MISSING_PREDICATE` | `findByStatusOrderByNameAsc` for a churn spreadsheet |
| `BillingService#byNumber` | `MISSING_PREDICATE` | `findByNumber`: numbers are unique, which is not isolation |
| `BillingService#allPayments` | `MISSING_PREDICATE` | `findAll()` on a tenant-scoped table |
| `ReportingDao#settlementSummary` | `MISSING_PREDICATE` | the second branch of a `UNION ALL` lost the tenant |

Two of these are worth naming separately, because a human reviewer usually misses them:

- `searchAnyTenant` — the SQL is assembled by MyBatis at runtime, and the same mapper statement is
  safe on one call and unsafe on the next. Only checking the executed statement finds this.
- `settlementSummary` — the first branch of the `UNION ALL` carries `p.tenant_id = ?`, so the
  statement looks filtered at a glance; the second branch sums every tenant's open invoices.

### False positives — 4

| Origin | SQL (shortened) | Why QueryFence is wrong |
|---|---|---|
| `OrderService#withItems` | `... from purchase_order po1_0 join order_item i1_0 on po1_0.id=i1_0.order_id where po1_0.tenant_id=? and po1_0.status=?` | fetch join. Every `order_item` row is reachable only through a `purchase_order` row that *is* fenced, so the statement is safe; the engine wants the filter on the child's own alias. Documented: ORM associations. |
| `OrderService#countItemsLazily` | `select ... from order_item i1_0 where i1_0.order_id=?` | the lazy association load of the same aggregate: the parent was fetched with `tenant_id = ?` in an earlier statement, and the engine sees one statement at a time. Documented. |
| `ReportingDao#orderTotalsFromSubquery` | `select t.status, sum(t.total) from (select o.status, o.total, o.tenant_id from purchase_order o where o.deleted_at is null) t where t.tenant_id = ?` | the derived table exposes `tenant_id` and the outer query filters on it, so no other tenant's row reaches the result. v0.1 does not push predicates down. Documented. |
| `ReportingDao#renumberInvoices` | `update invoice set number = cast(? as varchar) \|\| '-' \|\| cast(id as varchar) where number is null` | valid Postgres, reported `UNPARSEABLE`. JSqlParser 5.4 cannot read an **unqualified column named `number`** in an expression (`select number from invoice` fails; `select i.number from invoice i` parses). Not documented. |

The first three share two structural fixes the guide already recommends: map the tenant on the child
entity (`@TenantId` on `OrderItem`) and move the filter inside the subquery. Do both and this
application's false positives drop to the one parser bug.

### Deliberate exception — 1

`ReportingDao#platformRevenue`, the operator dashboard's revenue-per-tenant report. The suppression
from the guide works as written and takes the count from 20 to 19:

```yaml
suppressions:
  - rule: tenant-isolation
    origin: com.northwind.shop.report.ReportingDao#platformRevenue
    reason: Platform-wide revenue for the operator dashboard; runs as the operator, never in a tenant request.
```

## What the engine must fix before 0.1.0

### 1. `onUnparseable` does nothing (blocker)

`Policy.onUnparseable()` is parsed, validated and stored — and never read. The only decision point
is `policy.mode()`, in `QueryFenceExtension` and `QueryFenceTestExecutionListener`. With
`mode: FAIL` and `onUnparseable: REPORT` the unparseable statement above fails the build, and the
message tells the user to do what they have already done:

```
[parser] UNPARSEABLE
  problem : QueryFence could not parse this statement, so it cannot prove it safe.
            Report the SQL to QueryFence, or set onUnparseable: REPORT to only report it.
```

`README.md`, `docs/DESIGN.md`, `docs/ADOPTION.md` and `CHANGELOG.md` all promise this knob. Either
honour it at the failure decision, or remove it from the policy and every document.

### 2. An unparseable statement hides which tables it touches

The `UNPARSEABLE` finding carries `table: null`, so `--by table` shows `(not a table)` and the
triage list never says that the statement in question is an `UPDATE` on `invoice` with no tenant
filter — a real leak, invisible behind a parser failure. A textual match of the protected table
names against the raw SQL would be enough: "mentions `invoice`; not checked".

### 3. The console summary never reaches a Maven user

`RunReport.flush()` prints the summary from a JVM shutdown hook. Under Surefire the forked JVM's
output channel is already closed, so nothing is printed: `grep -i queryfence` over a full
`./mvnw test` log and over `target/surefire-reports/` finds nothing, while `report.json` is written
correctly. `docs/ADOPTION.md` step 3 promises "a summary per policy and a file", and a first-time
user sees only silence. Print at the end of the last test instead of at JVM shutdown.

### 4. `number` as a column name (upstream, needs documenting either way)

Of 24 common column names tried, only `number` fails to parse unqualified. It is an ordinary column
name in a billing schema, so it will be met. File it upstream, and until then say so under known
limitations with the workaround (qualify or quote the column).

### 5. Policy errors do not name the policy file

A suppression written as `origin: com.acme.Foo` (no `#method`) fails correctly and loudly, but the
message is `Suppression origin must be Class#method, got '...'` with no mention of
`queryfence.yml`, and it repeats once per test class. `PolicyYaml` wraps its own errors as
"QueryFence policy <resource> ..."; the ones thrown by the core model — `Suppression` here — escape
that wrapper, so exactly the mistakes a hand-written policy makes are the ones that do not name the
file.

### 6. Cosmetic: the origin of a lazy load is a lambda

`OrderService#lambda$countItemsLazily$0 (OrderService.java:104)` is accurate and still needs
explaining to a user. Resolving synthetic lambda frames to their enclosing method would read better.

## What was unclear or missing in ADOPTION.md

1. **Step 4 tells the reader to run `tools/queryfence-summary.py`, which they do not have.** It
   lives in this repository and is in no published artifact. Either link the raw URL with a
   `curl` line, or ship it (a `queryfence-tools` classifier, or print the same summary from the
   library).
2. **Adopting table by table is the missing advice.** With 15 real leaks you cannot switch to
   `FAIL`, and v0.1 has no baseline file. What actually works is to start `tables:` with one table,
   turn `FAIL` on for it, and add the next table — the guide should say so in step 6.
3. **"Nothing fails: REPORT mode only collects" is not quite true.** A malformed policy fails every
   test in the suite, before any SQL runs. Worth one sentence, because the first policy is the most
   likely to be malformed.
4. **The `information_schema` query returns tables that only sometimes carry a tenant.**
   `audit_log.tenant_id` is nullable here: platform jobs write rows with no tenant. The guide should
   say to review the list rather than paste it, and mention that a nullable tenant column means the
   rule will report the platform's own writes.
5. **Nothing about where origins come from, or what to do when they are wrong.** Origins were exact
   in this application, but `CaptureSettings.ofBasePackages(...)` exists for when they are not, and
   the Spring path has no way to set it: there is no YAML key for base packages, and
   `@QueryFencePolicy` does not take one. Either add the key or say plainly that the Spring path
   resolves origins automatically.
6. **The report path assumes a single-module build.** In a multi-module project the file is in
   `<module>/target/queryfence/report.json`, one per module, and the guide's copy-paste command
   finds nothing. One line fixes it.
7. **Step 7 promises something that does not exist**: "The report lists the [suppressions] that
   matched nothing". Nothing in `queryfence-report` tracks unmatched suppressions and the JSON has
   no such field. Implement it or drop the sentence.
