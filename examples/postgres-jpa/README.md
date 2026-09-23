# Example: Postgres + Spring Data JPA

```bash
docker compose up -d
../../mvnw verify
```

`InvoiceServiceTest.listsOpenInvoices` fails, because the SQL Hibernate generated for the derived
query `findByStatus` has no tenant filter:

```
QueryFence: 1 violation in com.acme.billing.InvoiceServiceTest#listsOpenInvoices

  [tenant-isolation] MISSING_PREDICATE
    table   : invoice
    problem : invoice (i1_0) has no tenant filter. Add "i1_0.tenant_id = ?" to the WHERE clause of the query block that uses it.
    sql     : select i1_0.id,i1_0.amount,i1_0.status,i1_0.tenant_id from invoice i1_0 where i1_0.status=?
    origin  : com.acme.billing.InvoiceService#openInvoices
```

Note what the origin points at: not the framework, but the service method that called the
repository. Nobody wrote that SQL; QueryFence checks it anyway.

The fix is in `InvoiceRepository`: use `findByTenantIdAndStatus`, and drop `findByStatus`. Then
`../../mvnw verify` passes.
