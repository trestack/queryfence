# Northwind Shop — the dogfood application

A multi-tenant shop service, written the way an application that already exists is written: a
service layer over Spring Data JPA, a MyBatis mapper for the search screen, hand-written SQL for
the reports, and a mix of careful and careless code. It exists for one reason — to run QueryFence
against something that was *not* designed to pass it.

It is **not part of the QueryFence reactor** and is never published. Nothing in `queryfence-*`
depends on it.

## What is in it

| Layer | Contents |
|---|---|
| Schema | `customer`, `purchase_order`, `order_item`, `invoice`, `payment`, `audit_log` (tenant-scoped) and `product`, `currency` (shared) |
| JPA | 7 entities, one lazy `@OneToMany`, sequences for the two tables that insert rows |
| Spring Data | 25 derived queries, 21 `@Query` methods (JPQL and native), `Specification` search, pagination, sort, fetch join, bulk `@Modifying` updates, soft delete |
| MyBatis | 5 statements with `<where>`, `<if>` and `<foreach>` |
| JdbcTemplate | 9 report statements: group by / having, CTE, derived table, `UNION ALL`, `IN (...)` |
| Tests | 62 tests in 8 classes on Postgres 17 via Testcontainers, one transaction per test |

Some of that code is deliberately sloppy in the ways real code is sloppy: a repository method that
forgot the tenant, a join added later without its tenant condition, a `findById` from a support
console, a bulk update written before the service became multi-tenant, an operator screen that
searches every tenant on purpose.

## Running it

Docker must be running; the tests start their own Postgres.

```bash
./mvnw -f dogfood/pom.xml test
python3 tools/queryfence-summary.py dogfood/target/queryfence/report.json --triage
```

The suite is green: QueryFence runs in `REPORT` mode, so it collects and never fails a test. The
policy is `dogfood/src/test/resources/queryfence.yml`, kept exactly as
[docs/ADOPTION.md](../docs/ADOPTION.md) says to write it on day one — no suppressions, nothing
tuned. What the run produces is written down in [DOGFOOD-REPORT.md](DOGFOOD-REPORT.md).

Do not "fix" the sloppy queries: they are the measurement.
