# Adopting QueryFence in an existing project

This is the path for a Spring Boot application that already exists and already has tests. The goal
of the first week is **not** a green build: it is an honest list of what your queries do today.
So you start in `REPORT` mode, read the report, and only then decide what to fix.

Budget: about an hour for steps 1 to 3, then as long as your triage list deserves.

## 1. Add the dependency

```xml
<dependency>
  <groupId>io.github.trestack</groupId>
  <artifactId>queryfence-spring-test</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

Nothing else changes: no annotation on your tests, no change to production code. Without a policy
file on the test classpath, the dependency does nothing at all.

## 2. Write the first policy

`src/test/resources/queryfence.yml`:

```yaml
version: 1
mode: REPORT          # start here. FAIL comes later, when the list is empty.
onUnparseable: REPORT # also start here: see what does not parse before it fails anyone

rules:
  - id: tenant-isolation
    type: require-predicate
    column: tenant_id
    tables:
      - purchase_order
      - order_item
      - invoice
```

Listing the tables is the only real work. Get the list from the schema, not from memory:

```sql
-- Postgres
SELECT table_name FROM information_schema.columns
WHERE column_name = 'tenant_id' AND table_schema = 'public' ORDER BY table_name;

-- MySQL
SELECT table_name FROM information_schema.columns
WHERE column_name = 'tenant_id' AND table_schema = DATABASE() ORDER BY table_name;
```

If your tenant column has another name, say so in `column`. If rows are looked up by something
other than `id`, set `primaryKey` on the rule.

Leave `update-without-where` and `delete-without-where` out for now. Add them once the tenant rule
is under control; they usually find a handful of fixture-cleanup statements and nothing else.

## 3. Run the tests you already have

```bash
./mvnw verify
```

Nothing fails: `REPORT` mode only collects. At the end of the run you get a summary per policy and
a file:

```
target/queryfence/report.json
```

The summary already tells you the shape of the problem. The file tells you where it is.

## 4. Read the report

```bash
python3 tools/queryfence-summary.py target/queryfence/report.json
python3 tools/queryfence-summary.py target/queryfence/report.json --triage
```

The first command groups the findings by rule, by table, by code and by origin. The second prints
one entry per origin — class, method and line — which is the list you actually work through.

Two numbers matter on day one:

- **statements checked.** If this is small, your test suite does not exercise much SQL, and
  QueryFence can only report on what runs. That is a fact about the test suite, not about
  QueryFence.
- **distinct origins.** Not findings: origins. Twenty findings from one repository method are one
  fix.

## 5. Triage: leak, false positive, or suppression

Go through the triage list and put every origin in one of three buckets.

### Real leak

The query really can return another tenant's rows. These are why you installed QueryFence.
Typical shapes:

| What the report says | What it means |
|---|---|
| `MISSING_PREDICATE` on a table you own | the query forgot the tenant |
| `PRIMARY_KEY_LOOKUP` | `findById` / `EntityManager.find`: an id is guessable |
| `NO_WHERE`, `TAUTOLOGICAL_WHERE` | an update or delete that hits every row |

Fix the query, or fix the mapping. For `PRIMARY_KEY_LOOKUP` on an entity, mapping the tenant with
Hibernate `@TenantId` fixes every load of that entity at once, including lazy ones.

### False positive

The statement is safe, but does not say so. In v0.1 this is almost always one of:

| Shape | Why | What to do |
|---|---|---|
| Child rows loaded by foreign key (`where order_id = ?`) | a lazy association or fetch join | map the tenant on the child entity (`@TenantId`), or protect only the aggregate root |
| A filter applied outside a derived table or CTE | v0.1 does not push predicates down | move the filter inside the subquery |
| A tenant-column equality with no anchor | `i.tenant_id = o.tenant_id` where `o` is not fenced either | fence the parent |
| `UNPARSEABLE` on SQL that is valid | the parser does not know that syntax | open an issue with the statement; suppress it meanwhile |

Count these. If they dominate, fix them structurally (usually: map the tenant on the entities)
rather than one suppression at a time.

### Deliberate exception

Cross-tenant admin screens, platform reports, migrations run from tests. These get a suppression,
and the reason is the point:

```yaml
suppressions:
  - rule: tenant-isolation
    origin: com.acme.admin.PlatformReportJob#nightlyTotals
    reason: Platform-wide revenue report; runs as the operator, never in a tenant request.
```

A suppression without a reason is a configuration error, on purpose. If you cannot write the
sentence, it is not an exception — it is a leak you have not looked at yet.

## 6. Turn it on

When the triage list is empty, or only holds documented suppressions:

```yaml
mode: FAIL
onUnparseable: FAIL
```

From then on the build fails on the query that broke the rule, pointing at the line that wrote it.

## 7. Keep it honest

- Do not switch QueryFence off to make a build green. It writes `"disabled": true` into the report
  and fails on CI unless someone sets `queryfence.allowDisabledInCi=true` deliberately.
- Add a test that exercises the query, rather than a suppression, whenever you can: QueryFence only
  sees SQL your tests run.
- Revisit suppressions when the code they name changes. The report lists the ones that matched
  nothing, which usually means the code moved.

## What QueryFence does not do

It verifies; it does not enforce. Keep your runtime protection — Postgres RLS, Hibernate
`@TenantId`, a MyBatis interceptor — and let QueryFence prove, in CI, that the SQL you actually run
carries the filter. *Enforce at runtime, verify in tests.*
