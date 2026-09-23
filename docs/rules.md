# Rules

Three rules ship in 0.1. Each one is specified clause by clause in the [design notes](DESIGN.md)
and covered by the golden corpus.

## `require-predicate`

Every occurrence of a protected table must be filtered by the tenant column.

```yaml
- id: tenant-isolation
  type: require-predicate
  column: tenant_id
  tables: [purchase_order, order_item]
  allowedFunctions: [current_setting]   # optional
  primaryKey: id                        # optional, default id
```

A statement passes when, for **every** occurrence of a protected table, the conditions that apply
to it prove the rows belong to one tenant:

| Accepted | Not accepted |
|---|---|
| `o.tenant_id = ?`, `= 42`, `= :tenant`, `= $1` | `o.tenant_id <> ?`, `> ?`, `LIKE ?` |
| `o.tenant_id IN (?, ?)` | `o.tenant_id IN (SELECT ...)`, `NOT IN` |
| `o.tenant_id = current_setting('app.tenant')` when listed in `allowedFunctions` | any other function call |
| `i.tenant_id = o.tenant_id` when `o` is fenced (transitive) | the same equality with nothing anchored |
| a predicate in `WHERE`, in an inner join's `ON`, or in the `ON` of the `LEFT JOIN` that introduces the table | the `ON` of a `LEFT JOIN` for the preserved side, `RIGHT`/`FULL JOIN` `ON`, `HAVING` |
| an `OR` where **every** branch fences the table | an `OR` where one branch does not |

Each subquery, CTE body and `UNION` branch is its own query block and is checked on its own.

## `update-without-where`

An `UPDATE` with no `WHERE`, or with a `WHERE` that is always true (`1 = 1`, `TRUE`,
`id = ? OR TRUE`), changes every row. Reported as `NO_WHERE` or `TAUTOLOGICAL_WHERE`.

```yaml
- id: no-unbounded-update
  type: update-without-where
```

## `delete-without-where`

The same for `DELETE`, including `DELETE ... USING` and MySQL multi-table deletes. `TRUNCATE` is
DDL and ignored: test fixtures use it all the time.

```yaml
- id: no-unbounded-delete
  type: delete-without-where
```

## Violation codes

| Code | Meaning |
|---|---|
| `MISSING_PREDICATE` | the table is read or written without a tenant filter |
| `PRIMARY_KEY_LOOKUP` | the only filter is the primary key: `findById`, `EntityManager.find` |
| `AMBIGUOUS_COLUMN` | an unqualified tenant column in a block with several tables |
| `MISSING_INSERT_COLUMN` | an `INSERT` that does not set the tenant column |
| `NO_WHERE` / `TAUTOLOGICAL_WHERE` | an unbounded `UPDATE`/`DELETE` |
| `UNSUPPORTED_STATEMENT` | a statement type not analysed yet (`MERGE`) on a protected table |
| `UNPARSEABLE` | SQL the parser does not understand, and that could touch rows |

Every message says what is wrong **and** how to fix it.
