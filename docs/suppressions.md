# Suppressions

Some queries really do cross tenants: a platform revenue report, an admin screen, a migration run
from a test. Those get a suppression — and a reason.

```yaml
suppressions:
  - rule: tenant-isolation
    origin: com.acme.admin.PlatformReportJob#nightlyTotals
    reason: Platform-wide revenue report; runs as the operator, never inside a tenant request.
```

## The rules of the game

- A suppression is keyed by `Class#method`, the same string the report prints as the origin. An
  origin without `#method` is a configuration error, not a prefix match.
- `rule:` names a rule id, or `parser` for the `UNPARSEABLE` findings the parser raises.
- **The reason is required.** A blank one is a configuration error and the policy refuses to load.
  If you cannot write the sentence, this is not an exception — it is a leak nobody has looked at.
- Suppressions live in the policy, never in production code. There is no annotation for your
  repositories to depend on, so nothing about QueryFence leaks into your application.
- A suppression that matched nothing during the run is listed in the console summary and in
  `report.json` under `unmatchedSuppressions`. That usually means the code moved, and the exception
  is no longer where it says it is.

## When *not* to suppress

| Situation | Better than a suppression |
|---|---|
| `PRIMARY_KEY_LOOKUP` on an entity | map the tenant with Hibernate `@TenantId`, or use `findByIdAndTenantId` |
| Child rows loaded by foreign key | map the tenant on the child entity, or protect only the aggregate root |
| A filter applied outside a derived table or CTE | move the filter inside the subquery |
| "The whole legacy package is noisy" | a second policy in `REPORT` mode for that package, via `@QueryFencePolicy` |
| "It is only the test fixture" | fixtures in `@BeforeEach` are outside the capture window already |

## Turning the checks off entirely

There is a switch, `queryfence.enabled=false`. It warns loudly, records itself in the report and
fails on CI unless someone sets `queryfence.allowDisabledInCi=true`. See
[Configuration](configuration.md#the-emergency-switch). Prefer a narrow suppression with a reason
over a switch nobody remembers.
