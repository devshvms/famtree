# famtree backend - local development and CI
#
# Everything a contributor needs is one `make` away. `make ci` runs exactly
# the same pipeline GitHub Actions runs, so red CI is reproducible locally.

SHELL := /bin/bash
MVN   := ./mvnw -B
COMPOSE := docker compose

.DEFAULT_GOAL := help

# ---------------------------------------------------------------- environment
.PHONY: help
help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

.PHONY: env
env: ## Create .env from .env.example if missing
	@test -f .env || (cp .env.example .env && echo "created .env from .env.example")

.PHONY: up
up: env ## Start Neo4j + MongoDB + Mailpit and seed the location tree
	$(COMPOSE) up -d --wait neo4j mongo mailpit
	$(COMPOSE) up neo4j-seed
	@echo ""
	@echo "  Neo4j browser  http://localhost:$${NEO4J_HTTP_PORT:-7474}  (neo4j / famtree-local-dev)"
	@echo "  Mongo          mongodb://localhost:$${MONGO_PORT:-27017}/famt"
	@echo "  Mailpit inbox  http://localhost:$${MAILPIT_UI_PORT:-8025}"
	@echo ""
	@echo "  Now run the API:  make run       (or 'make up-all' to containerise it too)"

.PHONY: up-all
up-all: env ## Start the databases AND the API in Docker
	$(COMPOSE) --profile app up -d --build --wait
	@echo "  API      http://localhost:$${APP_PORT:-8080}"
	@echo "  Swagger  http://localhost:$${APP_PORT:-8080}/swagger-ui.html"

.PHONY: down
down: ## Stop all containers (keeps data)
	$(COMPOSE) --profile app down

.PHONY: clean
clean: ## Stop containers AND delete database volumes
	$(COMPOSE) --profile app down -v
	$(MVN) clean

.PHONY: logs
logs: ## Tail logs from every container
	$(COMPOSE) --profile app logs -f

.PHONY: reseed
reseed: ## Wipe and reload the Earth/country location tree
	$(COMPOSE) exec -T neo4j cypher-shell -u neo4j -p "$${NEO4J_PASSWORD:-famtree-local-dev}" \
		'MATCH (l:Location) DETACH DELETE l'
	$(COMPOSE) up neo4j-seed

.PHONY: ps
ps: ## Show container status
	$(COMPOSE) --profile app ps

# ------------------------------------------------------------------- the app
.PHONY: run
run: ## Run the API on the host against the compose databases
	$(MVN) spring-boot:run

.PHONY: build
build: ## Package the jar (no tests)
	$(MVN) -DskipTests package

.PHONY: docker-build
docker-build: ## Build the application container image
	docker build -t famtree-be:local .

# ----------------------------------------------------------------- the tests
.PHONY: test
test: ## Unit tests only - no Docker required
	$(MVN) test

.PHONY: it
it: ## Integration tests against throwaway Testcontainers databases
	$(MVN) verify

.PHONY: coverage
coverage: ## Run all tests and open the JaCoCo report path
	$(MVN) verify
	@echo "coverage report: target/site/jacoco/index.html"

# -------------------------------------------------------------------- the CI
.PHONY: ci
ci: ## Run the full CI pipeline locally (identical to GitHub Actions)
	./scripts/ci.sh

.PHONY: secrets
secrets: ## Scan the working tree and history for committed credentials
	./scripts/scan-secrets.sh

.PHONY: verify-setup
verify-setup: ## Check that your machine has everything this repo needs
	./scripts/verify-setup.sh

.PHONY: secrets-history
secrets-history: ## Audit ALL git history for committed credentials (needs gitleaks)
	./scripts/scan-secrets.sh --history
