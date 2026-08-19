#!/usr/bin/env bash
# The CI pipeline. GitHub Actions calls this script and nothing else, so
# `make ci` on a laptop and a green check on a PR mean the same thing.
set -euo pipefail
cd "$(dirname "$0")/.."

# shellcheck disable=SC2034
BOLD=$'\033[1m'; RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RESET=$'\033[0m'

FAILED=()
step() { printf '\n%s==> %s%s\n' "$BOLD" "$1" "$RESET"; }
ok()   { printf '%s  ok%s  %s\n' "$GREEN" "$RESET" "$1"; }
skip() { printf '%s  skip%s %s\n' "$YELLOW" "$RESET" "$1"; }
fail() { printf '%s  FAIL%s %s\n' "$RED" "$RESET" "$1"; FAILED+=("$1"); }

run() { # run <label> <command...>
  local label="$1"; shift
  if "$@"; then ok "$label"; else fail "$label"; fi
}

have_docker() { docker info >/dev/null 2>&1; }

step "1/5  Compile"
run "compile" ./mvnw -B -DskipTests compile

step "2/5  Unit tests"
run "unit tests" ./mvnw -B test

step "3/5  Integration tests (Testcontainers)"
if have_docker; then
  if ./mvnw -B verify; then
    ok "integration tests"
  else
    fail "integration tests"
    # A blocked or offline registry looks like a test failure but isn't one.
    if grep -rqE 'ContainerFetchException|Could not find a valid Docker environment' \
        target/failsafe-reports/ 2>/dev/null; then
      printf '        (containers could not be fetched - check registry access, not the code)\n'
    fi
  fi
else
  skip "integration tests - no Docker daemon reachable"
fi

step "4/5  Secret scan"
run "secret scan" ./scripts/scan-secrets.sh

step "5/5  Container image builds"
if have_docker; then
  run "docker build" docker build -q -t famtree-be:ci .
else
  skip "docker build - no Docker daemon reachable"
fi

printf '\n%s―――――――――――――――――――――――――――――――%s\n' "$BOLD" "$RESET"
if [[ ${#FAILED[@]} -eq 0 ]]; then
  printf '%sCI passed%s\n' "$GREEN" "$RESET"
  exit 0
fi
printf '%sCI failed:%s\n' "$RED" "$RESET"
printf '  - %s\n' "${FAILED[@]}"
exit 1
