#!/usr/bin/env bash
# Tells a new contributor exactly what their machine is missing.
set -uo pipefail
cd "$(dirname "$0")/.."

GREEN=$'\033[32m'; RED=$'\033[31m'; YELLOW=$'\033[33m'; RESET=$'\033[0m'
missing=0

check() { # check <label> <test-cmd> <hint>
  if eval "$2" >/dev/null 2>&1; then
    printf '%s  ok%s    %s\n' "$GREEN" "$RESET" "$1"
  else
    printf '%s  MISSING%s %s  -> %s\n' "$RED" "$RESET" "$1" "$3"
    missing=1
  fi
}

optional() {
  if eval "$2" >/dev/null 2>&1; then
    printf '%s  ok%s    %s\n' "$GREEN" "$RESET" "$1"
  else
    printf '%s  optional%s %s  -> %s\n' "$YELLOW" "$RESET" "$1" "$3"
  fi
}

echo "Checking your machine for famtree development..."
echo ""
check "Java 17+"        "java -version 2>&1 | grep -qE '\"(1[7-9]|2[0-9])'" "install a JDK 17 or newer (sdkman: sdk install java 17.0.13-tem)"
check "Docker CLI"      "command -v docker"                                 "install Docker Desktop or Docker Engine"
check "Docker daemon"   "docker info"                                       "start Docker Desktop / systemctl start docker"
check "Docker Compose"  "docker compose version"                            "ships with Docker Desktop; else install the compose plugin"
check "Make"            "command -v make"                                   "apt install make / xcode-select --install"
check "Git"             "command -v git"                                    "install git"
echo ""
optional "gitleaks"     "command -v gitleaks"                               "brew install gitleaks - real secret scanning instead of the grep fallback"
optional "act"          "command -v act"                                    "brew install act - run the GitHub Actions workflow locally"
optional "jq"           "command -v jq"                                     "handy for poking the API from the shell"
echo ""

if [[ $missing -eq 0 ]]; then
  echo "${GREEN}You're ready. Run 'make up' then 'make run'.${RESET}"
else
  echo "${RED}Install the missing items above, then re-run 'make verify-setup'.${RESET}"
  exit 1
fi
