# Examples

Two small Spring Boot projects that show what QueryFence does to a build. Each one ships a query
that forgets the tenant filter, so `./mvnw verify` fails until you fix it — which is the point.

| Example | Stack |
|---|---|
| [`mysql-mybatis`](mysql-mybatis) | MySQL 8.4, MyBatis, dynamic SQL |
| [`postgres-jpa`](postgres-jpa) | Postgres 17, Spring Data JPA (Hibernate) |

## Running one

```bash
cd examples/postgres-jpa
docker compose up -d
../../mvnw verify
```

The build fails with the rule, the query and the line that wrote it. Fix the query the failure
points at, run `verify` again, and the build passes. `docker compose down` when you are done.

These projects are not part of the QueryFence build: they use the published coordinates and are
meant to be read and run on their own.
