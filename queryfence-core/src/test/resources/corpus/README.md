# Golden corpus

Data files of SQL statements and the violations QueryFence must report for them. The corpus is
the executable form of [docs/DESIGN.md](../../../../../docs/DESIGN.md): every case cites the clause
it exercises, and a disagreement between a case and DESIGN.md is a bug in one of them.

`GoldenCorpusTest` (a JUnit 5 parameterized test) loads every `*.yml` file in this directory except
`policies.yml`, runs each case through the rule engine and compares the result with `expect`.

## Files

| File | Topic |
|---|---|
| `policies.yml` | Named policies that cases refer to (`default`, `rls`, `schema`) |
| `basic.yml` | Single-table statements |
| `alias.yml` | Aliases, qualifiers and ambiguous columns |
| `identifiers.yml` | Quoting, case and schema-qualified names |
| `parameters.yml` | Parameters, literals, casts and IN lists |
| `allowed-functions.yml` | `allowedFunctions` on and off (RLS-style comparisons) |
| `join.yml` | Inner, comma and cross joins, self-joins |
| `outer-join.yml` | LEFT / RIGHT / FULL joins |
| `transitive.yml` | Tenant-column equalities and their anchors |
| `subquery.yml` | `EXISTS`, `IN`, scalar, HAVING, ORDER BY and nested subqueries |
| `derived-table.yml` | Derived tables |
| `or-bypass.yml` | Predicates under `OR` |
| `not-negation.yml` | `NOT`, `<>`, `NOT IN` |
| `union.yml` | Set operations |
| `cte.yml` | Common table expressions |
| `recursive-cte.yml` | `WITH RECURSIVE` branches |
| `update-delete.yml` | `UPDATE` and `DELETE`, including the without-where rules |
| `tautology.yml` | Always-true WHERE clauses |
| `insert.yml` | `INSERT` and `INSERT ... SELECT` |
| `mysql.yml` | MySQL multi-table writes, `INSERT ... SET`, `REPLACE` |
| `postgres.yml` | `UPDATE ... FROM`, `DELETE ... USING`, `ON CONFLICT`, `LATERAL` |
| `schema.yml` | A policy whose table entry carries a schema |
| `statements.yml` | Ignored, unsupported and unparseable statements |
| `comments.yml` | Predicates hidden in comments |

## Case format

```yaml
cases:
  - id: join-001                 # unique across the corpus: <topic>-<nnn>
    clause: [RP-1, RP-5]         # DESIGN.md clauses this case exercises
    policy: default              # a key in policies.yml
    dialect: ansi                # ansi | mysql | postgres
    sql: |-
      SELECT ...
    expect:                      # [] when the statement must pass
      - rule: tenant-isolation   # rule id from the policy
        code: MISSING_PREDICATE  # violation code from DESIGN.md "Violation model"
        table: order_item        # normalized table name
        alias: i                 # alias as written in the SQL, null when there is none
        message: '...'           # exact message, including how to fix it
    reason: One sentence explaining why.
```

Messages come from the fixed templates in DESIGN.md "Violation model" and are compared verbatim,
so every expected violation documents both the problem and the fix. Order of `expect` entries is
not significant. Every expected violation must be reported, and no other violation may be reported.

## Tests that read this corpus

| Test | What it checks |
|---|---|
| `GoldenCorpusTest` | The engine reports exactly the expected violations, messages included, and never throws |
| `CorpusStructureTest` | Unique ids, known clauses (parsed from `docs/DESIGN.md`), dialects, policies, fix hints in messages |
| `MetamorphicTest` | Removing any single tenant predicate from a passing case (on the parsed statement) must produce a violation |

## Adding cases

- Every rule change or bug fix adds cases here first (test-first).
- A new bypass is a security issue: see `SECURITY.md` before publishing the case.
- Keep SQL realistic: statements an ORM, a mapper or a developer would actually send.
