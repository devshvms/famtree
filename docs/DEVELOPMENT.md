# Development guide

Everything needed to build on this codebase locally.

## 1. Prerequisites

| Tool | Why | Install |
|---|---|---|
| JDK 17+ | the app targets Java 17 | [Temurin](https://adoptium.net) or `sdk install java 17.0.13-tem` |
| Docker + Compose | Neo4j, MongoDB, Mailpit, Testcontainers | [Docker Desktop](https://docs.docker.com/get-docker/) |
| Make | task runner | preinstalled on macOS/Linux |
| gitleaks *(optional)* | real secret scanning | `brew install gitleaks` |
| act *(optional)* | run the GitHub Actions workflow locally | `brew install act` |

```bash
make verify-setup    # tells you exactly what is missing
```

## 2. First run

```bash
git clone https://github.com/devshvms/famtree && cd famtree
make up      # starts databases, seeds Earth + ~250 countries
make run     # starts the API on :8080
```

| Service | URL | Credentials |
|---|---|---|
| API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| Neo4j browser | http://localhost:7474 | `neo4j` / `famtree-local-dev` |
| MongoDB | `mongodb://localhost:27017/famt` | none |
| Mailpit inbox | http://localhost:8025 | — |

To run the API in Docker too: `make up-all`.

### Why Mailpit matters

Standard-user onboarding mails a 6-digit PIN, then a 6-digit OTP. Without a
mail server you cannot read either, so the flow is untestable. Mailpit accepts
any credentials, sends nothing to the outside world, and shows every message at
`localhost:8025`.

## 3. Daily commands

```bash
make test        # unit tests, seconds, no Docker
make it          # integration tests against real databases
make ci          # the whole pipeline, exactly as GitHub Actions runs it
make secrets     # scan for committed credentials
make logs        # tail container logs
make reseed      # rebuild the country hierarchy
make clean       # stop everything and delete the volumes
```

## 4. How CI works

`scripts/ci.sh` **is** the pipeline. GitHub Actions calls the same steps, so a
red build is always reproducible with `make ci`:

1. compile
2. unit tests (Surefire, `*Test`)
3. integration tests (Failsafe, `*IT`, Testcontainers) — skipped when no Docker
4. secret scan
5. container image build

`.github/workflows/ci.yml` runs these as three parallel jobs (build, secrets,
image) on every push to `dev`/`main` and every pull request, and uploads test
and coverage reports as artifacts.

To run the workflow itself locally: `act pull_request`.

## 5. Writing tests

Unit tests end in `Test` and must not need Docker. Integration tests end in
`IT` and extend `support/AbstractIntegrationTest`, which starts one Neo4j and
one MongoDB container shared across the whole test run:

```java
class MyFeatureIT extends AbstractIntegrationTest {
    @Autowired PersonService personService;

    @Test
    void doesTheThing() { ... }
}
```

Because so much behaviour lives in Cypher traversals, prefer an `IT` over a
mock-based unit test for anything that touches a repository.

Coverage lands in `target/site/jacoco/index.html` after `make coverage`.

## 6. Secrets

`application.properties` contains no credentials — every value reads
`${ENV_VAR:default}` and the defaults target the local compose stack. Real
values go in `.env` (git-ignored) or the environment.

> **Outstanding:** a real Neo4j password was committed to this repository's
> history and pushed publicly. It must be rotated at the provider, and the
> history rewritten or the database considered compromised. `make secrets`
> guards the working tree; `make secrets-history` audits the past.

## 7. Known rough edges

Worth knowing before you build on top — see the review notes for detail.

- `UserController` has no authorization checks and returns password hashes.
- `POST /api/auth/register-admin` is public and does not check whether a tenant
  already has an admin.
- The activation endpoints (`first-login`, `verify-otp`, `resend-otp`,
  `set-password`) are not in the `permitAll` list, so the users who need them
  cannot reach them.
- Bean validation annotations are inert (no Hibernate Validator on the classpath).
- No CORS configuration, so browser clients are blocked.
- `LocationService.updateLocation` regenerates the ID by delete-and-recreate,
  which drops every relationship pointing at the node.
- Account lockout is implemented but unreachable from the real login path.
