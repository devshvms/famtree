# famtree backend (`in.shvms:famt-be`)

Multi-tenant genealogy API. Spring Boot 3.4 / Java 17 / Maven.

## Getting running

```bash
make verify-setup   # checks Java, Docker, compose, make
make up             # Neo4j + MongoDB + Mailpit, seeds Earth + ~250 countries
make run            # the API on http://localhost:8080
```

Swagger: `http://localhost:8080/swagger-ui.html` · Neo4j browser: `http://localhost:7474`
· Mailpit (catches every OTP and welcome-PIN email): `http://localhost:8025`

`make help` lists everything. `make ci` runs the exact pipeline GitHub Actions runs.

## Architecture

Two datastores, split by shape of the data:

- **Neo4j** — `Person`, `Location`, `Event`, `Lineage`, `Group` nodes and the
  relationships between them. Everything genealogical.
- **MongoDB** — `Tenant`, `User`, `AuditLog`. Identity, billing and history.

Request path: `controller/` → `service/` → `repositories/{neo4j,mongo}/`, with
`util/EntityMapper` converting between `dto/` records and `entity/` classes.
`config/` holds Spring Security, the JWT filter and `SecurityContextHelper`
(the way to get the current tenant/user — never trust the `{tenantId}` path
variable alone; controllers call `validateTenantAccess`).

### Things that will surprise you

- **`Person.childrenRelations` holds parents, not children.** It maps an
  *incoming* `PARENT_CHILD` edge whose `@TargetNode` is the parent. `getParents()`
  reads it; `getChildren()` scans the tenant. Read `PersonService` before
  touching relationship code.
- **Locations are global, not tenant-scoped.** One hierarchy shared by every
  tenant: `Earth → COUNTRY → STATE → DISTRICT → TOWN → VILLAGE`. IDs are
  generated from the hierarchy (`sta_india_karna`), so renaming a location
  changes its ID.
- **Countries and the Earth node are seeded, never created via the API.**
  `src/main/resources/init/Countries` is the source of truth; `make up` loads it
  and `make reseed` reloads it. The script starts with `DETACH DELETE`, so the
  seed job guards on Earth already existing.
- **Bean validation is not active.** `jakarta.validation-api` is on the
  classpath but Hibernate Validator is not, so `@NotNull`/`@Past`/`@Min` do
  nothing. Only the hand-written `dto.validate()` methods run. Add
  `spring-boot-starter-validation` if you want the annotations to bite.
- **Two generations of code coexist.** `UserController` is pre-security-rewrite:
  no `@PreAuthorize`, no tenant check, `getActingUserId()` returns `"system"`.
  `PersonController`/`EventController`/`LineageController` are the current
  pattern. Follow the latter; don't copy the former.

## Testing

```bash
make test    # unit tests, no Docker needed
make it      # integration tests on throwaway Neo4j + Mongo (Testcontainers)
```

`*Test` = unit, runs under Surefire. `*IT` = integration, runs under Failsafe
and extends `support/AbstractIntegrationTest`, which boots real containers.
Prefer an `*IT` for anything touching a repository — Cypher mapping bugs do not
show up against mocks.

## Configuration

Every value in `application.properties` is `${ENV_VAR:default}`, with defaults
pointing at the compose stack. Never commit a real credential: `make secrets`
gates on it, and CI fails the build. Copy `.env.example` to `.env` to override.

## Conventions

- Controllers return `ResponseEntity<?>` and map exceptions to status codes
  themselves; `IllegalArgumentException` → 400, `SecurityException` → 403,
  `RuntimeException` → 404.
- Every mutating service method writes an `AuditLog` via its private `logAudit`.
- Services take `(tenantId, actingUserId, ...)` as their first parameters.
