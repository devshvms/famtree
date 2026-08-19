#!/bin/bash
# SessionStart hook: prepares a Claude Code on the web session so that
# compiling, testing and linting work immediately.
#
# Deliberately never fails the session - every step degrades to a warning,
# because a half-warmed container is better than a session that won't start.
set -uo pipefail

# Local sessions already have a working machine; only the remote ones need this.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$PROJECT_DIR" || exit 0

echo "[session-start] preparing famtree backend workspace"

# 1. Warm the Maven repository. This is the big win: without it the first
#    `mvn test` in a session spends minutes downloading Spring Boot.
if [ -x ./mvnw ]; then
  echo "[session-start] resolving Maven dependencies (this is cached for later sessions)"
  if ./mvnw -B -ntp -q dependency:go-offline; then
    echo "[session-start] maven dependencies ready"
  else
    echo "[session-start] WARNING: dependency resolution incomplete - builds may download on demand"
  fi
  # Pre-compile so the first edit-test loop is fast.
  ./mvnw -B -ntp -q -DskipTests compile >/dev/null 2>&1 \
    && echo "[session-start] main sources compiled" \
    || echo "[session-start] WARNING: initial compile failed - check the working tree"
fi

# 2. Integration tests need a Docker daemon for Testcontainers. Start one if
#    the binary exists and nothing is listening yet.
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    echo "[session-start] docker daemon already running"
  elif command -v dockerd >/dev/null 2>&1; then
    echo "[session-start] starting docker daemon"
    (dockerd >/tmp/dockerd.log 2>&1 &)
    for _ in $(seq 1 15); do
      docker info >/dev/null 2>&1 && break
      sleep 1
    done
    if docker info >/dev/null 2>&1; then
      echo "[session-start] docker daemon ready - 'make it' can run integration tests"
    else
      echo "[session-start] WARNING: docker daemon did not start; see /tmp/dockerd.log"
      echo "[session-start]          unit tests ('make test') still work"
    fi
  fi
else
  echo "[session-start] docker not available - integration tests will be skipped"
fi

# 3. Give the session the env file the compose stack expects.
if [ ! -f .env ] && [ -f .env.example ]; then
  cp .env.example .env
  echo "[session-start] created .env from .env.example"
fi

# 4. Export settings the agent benefits from for the rest of the session.
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo 'export MAVEN_ARGS="-B -ntp"'
    echo 'export TESTCONTAINERS_RYUK_DISABLED=true'
  } >> "$CLAUDE_ENV_FILE"
fi

echo "[session-start] done - try 'make help'"
exit 0
