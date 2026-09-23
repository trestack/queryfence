# Example: MySQL + MyBatis

```bash
docker compose up -d
../../mvnw verify
```

`OrderServiceTest.listsTheOpenOrdersOfOneTenant` fails:

```
QueryFence: 1 violation in com.acme.shop.OrderServiceTest#listsTheOpenOrdersOfOneTenant

  [tenant-isolation] MISSING_PREDICATE
    table   : purchase_order
    problem : purchase_order has no tenant filter. Add "purchase_order.tenant_id = ?" to the WHERE clause of the query block that uses it.
    sql     : SELECT id, status, total FROM purchase_order WHERE status = ?
    origin  : com.acme.shop.OrderMapper#findByStatus
```

The fix is in `src/main/resources/mappers/OrderMapper.xml`: add `AND tenant_id = #{tenantId}` to
`findByStatus`. Then `../../mvnw verify` passes.

Note that the other test passes: QueryFence only complains about the query that is actually
missing its filter, and it names the mapper method that produced it.
