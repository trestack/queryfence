# Configuration

## The policy file

`src/test/resources/queryfence.yml`, or another file named with `@QueryFencePolicy`.

```yaml
version: 1                       # required; only 1 exists
mode: FAIL                       # FAIL | REPORT, default FAIL
onUnparseable: FAIL              # FAIL | REPORT, default FAIL

rules:
  - id: tenant-isolation         # required, unique
    type: require-predicate      # require-predicate | update-without-where | delete-without-where
    column: tenant_id            # require-predicate only, required
    tables: [purchase_order]     # require-predicate only, required, non-empty
    allowedFunctions: []         # optional: functions accepted as a value, e.g. current_setting
    primaryKey: id               # optional, default id; drives PRIMARY_KEY_LOOKUP

suppressions:
  - rule: tenant-isolation
    origin: com.acme.Foo#bar     # required, Class#method
    reason: why this is safe     # required, non-blank
```

Unknown keys, unknown rule types, a wrong version, a duplicate rule id or a suppression without a
reason are configuration errors: QueryFence refuses to load the policy and says which key is wrong.

## Modes

| Setting | Effect |
|---|---|
| `mode: FAIL` | a violation fails the test |
| `mode: REPORT` | violations are collected and printed, nothing fails |
| `onUnparseable: FAIL` | SQL that does not parse is a violation |
| `onUnparseable: REPORT` | it is only reported |

Start a new adoption in `REPORT`; see [Adopting in an existing project](ADOPTION.md).

## In Java

```java
Policy policy = Policy.builder()
    .requirePredicate("tenant-isolation", "tenant_id", "purchase_order", "order_item")
    .updateWithoutWhere("no-unbounded-update")
    .suppress("tenant-isolation", "com.acme.admin.ReportJob#nightly", "Platform report")
    .mode(Mode.REPORT)
    .build();

QueryFenceExtension.of(policy);
```

## Origin resolution

By default the origin is the first stack frame outside the JDK, drivers, ORMs, frameworks and
QueryFence. To make it exact, name your packages:

```java
QueryFenceExtension.of(policy, CaptureSettings.ofBasePackages("com.acme"));
```

## The emergency switch

`queryfence.enabled=false` (a Spring property, or `-Dqueryfence.enabled=false` for the JUnit
extension) switches the checks off for a run. QueryFence then prints a loud warning, writes
`"disabled": true` with the reason into the report, and **fails the build when the `CI`
environment variable is set** unless `queryfence.allowDisabledInCi=true` is also set.

It is an emergency exit, not a configuration option. To accept one known query, write a
[suppression](suppressions.md) with a reason.
