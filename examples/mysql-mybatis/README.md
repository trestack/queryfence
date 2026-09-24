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
    origin  : com.acme.shop.OrderService#openOrdersOf (OrderService.java:21)
```

The fix is in `src/main/resources/mappers/OrderMapper.xml`: add `AND tenant_id = #{tenantId}` to
`findByStatus`. Then `../../mvnw verify` passes.

Note two things. The assertion in that test **passes**: the fixture has one OPEN order and it
belongs to tenant 7, so the leak is invisible in the data — exactly how these bugs survive for
years. And the origin is the application method that asked for the query, not MyBatis: the mapper
is a proxy, so QueryFence reports the first frame of your own code.
