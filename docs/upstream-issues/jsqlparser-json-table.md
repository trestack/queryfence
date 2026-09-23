# JSqlParser and `JSON_TABLE`: investigated, no issue to file

**Status: no bug. Do not file this.**

## What we thought

While hardening the corpus we found that this statement is reported as `UNPARSEABLE`:

```sql
SELECT JSON_TABLE(o.data, '$' COLUMNS(k INT PATH '$.k')) FROM purchase_order o
```

That looked like a JSqlParser gap worth reporting, because `JSON_TABLE` is standard in MySQL 8.0+,
Oracle 12c+ and MariaDB 10.6+.

## What we found

The statement above is not valid SQL. `JSON_TABLE` is a **table function**: it may only appear
where a row source is expected, never in a SELECT list or a WHERE clause. MySQL and Oracle reject
it there too, so JSqlParser is right to reject it, and QueryFence is right to report it fail
closed — the database would fail on that statement anyway.

The forms applications actually write do parse with JSqlParser 5.4:

```sql
-- parses
SELECT jt.k FROM purchase_order o, JSON_TABLE(o.data, '$' COLUMNS(k INT PATH '$.k')) AS jt;

-- parses
SELECT jt.k FROM purchase_order o
JOIN JSON_TABLE(o.data, '$' COLUMNS(k INT PATH '$.k')) AS jt ON TRUE;
```

QueryFence treats the table function as a row source that is never a protected table, and still
requires a tenant filter on `purchase_order`. Both forms are covered by corpus cases in
`mysql.yml`.

## What to do instead

Nothing upstream. If a **valid** DML statement is ever reported as `UNPARSEABLE`, write the report
in this directory following the template in `README.md`, file it, and add corpus cases once the fix
is released.
